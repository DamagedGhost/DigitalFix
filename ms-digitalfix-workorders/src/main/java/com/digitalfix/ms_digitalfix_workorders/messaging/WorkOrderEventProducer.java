package com.digitalfix.ms_digitalfix_workorders.messaging;

import com.digitalfix.ms_digitalfix_workorders.config.RabbitProducerProperties;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor de eventos hacia RabbitMQ.
 * Decide qué mensajes salen según el nuevo estado de la orden:
 *  - ASIGNADA          -> email al cliente + ticket de despacho al técnico
 *  - EN_DESPLAZAMIENTO -> email al cliente
 *  - CERRADA           -> email al cliente + generación de PDF
 */
@Component
public class WorkOrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderEventProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProducerProperties props;

    public WorkOrderEventProducer(RabbitTemplate rabbitTemplate, RabbitProducerProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publishStatusChange(WorkOrder order) {
        WorkOrderStatus status = order.getStatus();

        switch (status) {
            case ASIGNADA -> {
                send(props.routingKeys().email(), order);
                send(props.routingKeys().dispatch(), order);
            }
            case EN_DESPLAZAMIENTO -> send(props.routingKeys().email(), order);
            case CERRADA -> {
                send(props.routingKeys().email(), order);
                send(props.routingKeys().report(), order);
            }
            default -> {
                // los demás estados no generan mensajes
            }
        }
    }

    private void send(String routingKey, WorkOrder order) {
        try {
            EventEnvelope envelope = new EventEnvelope(
                    "WORKORDER_" + order.getStatus().name(),
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    UUID.randomUUID().toString(),
                    "workorder-" + order.getId(),
                    new WorkOrderEventPayload(
                            order.getId(),
                            order.getClientName(),
                            order.getTechnicianName(),
                            order.getStatus().name()));

            rabbitTemplate.convertAndSend(props.exchange(), routingKey, envelope);
            log.info("Mensaje enviado a RabbitMQ: exchange={}, routingKey={}, orden={}",
                    props.exchange(), routingKey, order.getId());
        } catch (Exception e) {
            // Si RabbitMQ falla, el cambio de estado igual queda guardado
            log.error("No se pudo enviar mensaje a RabbitMQ (routingKey={}, orden={}): {}",
                    routingKey, order.getId(), e.getMessage());
        }
    }
}