package com.digitalfix.ms_digitalfix_notify.admin;

import com.digitalfix.ms_digitalfix_notify.config.DigitalfixRabbitProperties;
import com.digitalfix.ms_digitalfix_notify.config.DigitalfixRabbitProperties.QueueDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.event.WorkOrderEventPayload;

/**
 * Administración de las colas: consultar, crear, borrar, purgar y reprocesar la DLQ.
 * Los nombres salen del application.yaml (DigitalfixRabbitProperties).
 */
@Service
public class AdminQueueService {

    private static final Logger log = LoggerFactory.getLogger(AdminQueueService.class);

    // Solo letras, números, punto, guion y guion bajo (sin espacios)
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int LARGO_MAXIMO = 100;

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final DigitalfixRabbitProperties props;

    public AdminQueueService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate,
                             DigitalfixRabbitProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    /** Estado de las 6 colas del sistema (3 principales y 3 DLQ). */
    public List<QueueStatus> listar() {
        List<QueueStatus> result = new ArrayList<>();
        for (String flow : List.of("email", "dispatch", "report")) {
            QueueDef def = definicion(flow);
            result.add(estado(flow, "principal", def.name()));
            result.add(estado(flow, "dlq", def.dlq()));
        }
        return result;
    }

    /** Estado de una cola cualquiera por su nombre. 404 si no existe. */
    public QueueStatus consultar(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cola no existe: " + name);
        }
        return new QueueStatus("custom", "custom", name, info.getMessageCount(), info.getConsumerCount());
    }

    /** Crea una cola durable y la conecta al exchange indicado con su routing key. */
    public QueueStatus crear(CreateQueueRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el cuerpo de la petición");
        }

        String name = validarNombre(request.name());

        String routingKey = request.routingKey();
        if (routingKey == null || routingKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La routing key es obligatoria");
        }
        routingKey = routingKey.trim();

        String exchange = (request.exchange() == null || request.exchange().isBlank())
                ? "direct" : request.exchange().trim().toLowerCase();
        if (!exchange.equals("direct") && !exchange.equals("topic")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El exchange debe ser 'direct' o 'topic'");
        }

        if (amqpAdmin.getQueueInfo(name) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cola ya existe: " + name);
        }

        Queue queue = new Queue(name, true);
        amqpAdmin.declareQueue(queue);
        if (exchange.equals("direct")) {
            amqpAdmin.declareBinding(BindingBuilder.bind(queue)
                    .to(new DirectExchange(props.exchanges().direct())).with(routingKey));
        } else {
            amqpAdmin.declareBinding(BindingBuilder.bind(queue)
                    .to(new TopicExchange(props.exchanges().topic())).with(routingKey));
        }

        log.info("[ADMIN] Cola '{}' creada en el exchange {} con la llave '{}'", name, exchange, routingKey);
        return new QueueStatus("custom", "custom", name, 0, 0);
    }

    /** Borra una cola. No deja borrar las colas del sistema. */
    public void borrar(String name) {
        if (colasProtegidas().contains(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede borrar '" + name + "': es una cola del sistema");
        }
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cola no existe: " + name);
        }
        amqpAdmin.deleteQueue(name);
        log.warn("[ADMIN] Cola '{}' borrada", name);
    }

    /** Vacía la cola principal o la DLQ de un flujo. Devuelve cuántos mensajes borró. */
    public int purgar(String flow, boolean dlq) {
        QueueDef def = definicion(flow);
        String queue = dlq ? def.dlq() : def.name();
        int borrados = amqpAdmin.purgeQueue(queue);
        log.warn("[ADMIN] Cola '{}' purgada: {} mensajes eliminados", queue, borrados);
        return borrados;
    }

    /**
     * Saca mensajes de la DLQ y los vuelve a mandar a la cola principal
     * (por el exchange direct, con la misma llave que usa workorders).
     * Solo mueve los mensajes que había al empezar, así un mensaje que vuelve
     * a fallar y regresa a la DLQ no se procesa en bucle.
     * Devuelve cuántos mensajes se reprocesaron.
     */
    public int reprocesar(String flow, int max) {
        QueueDef def = definicion(flow);

        QueueInformation info = amqpAdmin.getQueueInfo(def.dlq());
        long enDlq = (info == null) ? 0 : info.getMessageCount();
        int aMover = (int) Math.min(enDlq, max);

        int movidos = 0;
        while (movidos < aMover) {
            Message message = rabbitTemplate.receive(def.dlq(), 500);
            if (message == null) {
                break; // la DLQ quedó vacía
            }
            try {
                rabbitTemplate.send(props.exchanges().direct(), def.directKey(), message);
                movidos++;
            } catch (Exception e) {
                // Si no se pudo reenviar, el mensaje vuelve a la DLQ para no perderlo
                log.error("[ADMIN] No se pudo reprocesar un mensaje de '{}': {}", def.dlq(), e.getMessage());
                rabbitTemplate.send(props.exchanges().dead(), def.dlqKey(), message);
                break;
            }
        }
        log.info("[ADMIN] Reproceso de '{}': {} mensajes enviados de vuelta a '{}'",
                def.dlq(), movidos, def.name());
        return movidos;
    }

    /**
     * Simula un fallo inyectando un mensaje inválido a la cola 'q.cmd.dispatch'.
     * Al no tener técnico asignado, DispatchTicketService lanzará UnrecoverableMessageException
     * y ConsumerAckSupport ejecutará basicNack(requeue=false), enviándolo directamente a 'q.cmd.dispatch.dlq'.
     */
    public Map<String, Object> simularFalloDlq() {
        String eventId = UUID.randomUUID().toString();
        long orderId = 9999L;
        EventEnvelope faultyEnvelope = new EventEnvelope(
                "WORKORDER_ASIGNADA",
                eventId,
                Instant.now().toString(),
                UUID.randomUUID().toString(),
                "sim-dlq-" + orderId,
                new WorkOrderEventPayload(orderId, "Cliente Demostración DLQ", null, "ASIGNADA")
        );

        rabbitTemplate.convertAndSend(
                props.exchanges().direct(),
                props.queues().dispatch().directKey(),
                faultyEnvelope
        );

        log.warn("[ADMIN][SIMULACION] Evento defectuoso publicado a exchange '{}' con routingKey '{}'. " +
                 "Orden sin técnico -> provocará desvío automático a DLQ ('{}')",
                props.exchanges().direct(), props.queues().dispatch().directKey(), props.queues().dispatch().dlq());

        return Map.of(
                "status", "simulated",
                "message", "Mensaje defectuoso inyectado a " + props.queues().dispatch().name() + ". Se desvía automáticamente a " + props.queues().dispatch().dlq(),
                "eventId", eventId,
                "targetQueue", props.queues().dispatch().name(),
                "expectedDlq", props.queues().dispatch().dlq()
        );
    }

    // ---------- Helpers ----------

    private String validarNombre(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la cola es obligatorio");
        }
        String limpio = name.trim();
        if (limpio.length() > LARGO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los " + LARGO_MAXIMO + " caracteres");
        }
        if (!NOMBRE_VALIDO.matcher(limpio).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre solo puede tener letras, números, punto, guion y guion bajo (sin espacios)");
        }
        return limpio;
    }

    // Las 6 colas que usa el sistema: no se pueden borrar
    private Set<String> colasProtegidas() {
        return Set.of(
                props.queues().email().name(), props.queues().email().dlq(),
                props.queues().dispatch().name(), props.queues().dispatch().dlq(),
                props.queues().report().name(), props.queues().report().dlq());
    }

    private QueueStatus estado(String flow, String type, String queue) {
        QueueInformation info = amqpAdmin.getQueueInfo(queue);
        if (info == null) {
            return new QueueStatus(flow, type, queue, 0, 0);
        }
        return new QueueStatus(flow, type, queue, info.getMessageCount(), info.getConsumerCount());
    }

    // Convierte "email" / "dispatch" / "report" en su definición del yaml
    private QueueDef definicion(String flow) {
        return switch (flow.toLowerCase()) {
            case "email" -> props.queues().email();
            case "dispatch" -> props.queues().dispatch();
            case "report" -> props.queues().report();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Flujo desconocido: " + flow + " (usa email, dispatch o report)");
        };
    }
}