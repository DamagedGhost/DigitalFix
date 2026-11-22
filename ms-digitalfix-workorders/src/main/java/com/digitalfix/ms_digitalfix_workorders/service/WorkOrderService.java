package com.digitalfix.ms_digitalfix_workorders.service;

import com.digitalfix.ms_digitalfix_workorders.dto.StatusUpdateRequest;
import com.digitalfix.ms_digitalfix_workorders.dto.WorkOrderRequest;
import com.digitalfix.ms_digitalfix_workorders.messaging.WorkOrderEventProducer;
import com.digitalfix.ms_digitalfix_workorders.messaging.WorkOrderKafkaProducer;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import com.digitalfix.ms_digitalfix_workorders.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio principal de órdenes de trabajo.
 * Guarda primero en PostgreSQL y recién después avisa a RabbitMQ (comandos a notify)
 * y a Kafka (evento de streaming a report). Así, si alguno de los brokers está caído,
 * la orden igual queda guardada y la operación del usuario no se cae.
 */
@Service
public class WorkOrderService {

    private final WorkOrderRepository repository;
    private final WorkOrderEventProducer eventProducer;
    private final WorkOrderKafkaProducer kafkaProducer;

    public WorkOrderService(WorkOrderRepository repository,
                            WorkOrderEventProducer eventProducer,
                            WorkOrderKafkaProducer kafkaProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.kafkaProducer = kafkaProducer;
    }

    // Crea la orden siempre en estado inicial CREADA y publica el evento en Kafka
    // para que el microservicio de reportes la contabilice en tiempo real.
    public WorkOrder create(WorkOrderRequest request) {
        WorkOrder order = new WorkOrder();
        order.setClientName(request.getClientName());
        order.setDescription(request.getDescription());
        order.setStatus(WorkOrderStatus.CREADA);
        WorkOrder saved = repository.save(order);

        // Avisa por Kafka que se creó la orden (si falla, no afecta la creación)
        kafkaProducer.publish(saved);
        return saved;
    }

    public WorkOrder findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + id));
    }

    // Consulta flexible: combina filtro por estado y/o rango de fechas según lo que llegue en la URL
    public List<WorkOrder> findAll(WorkOrderStatus status, LocalDateTime from, LocalDateTime to) {
        if (status != null && from != null && to != null) {
            return repository.findByStatusAndCreatedAtBetween(status, from, to);
        }
        if (status != null) {
            return repository.findByStatus(status);
        }
        if (from != null && to != null) {
            return repository.findByCreatedAtBetween(from, to);
        }
        return repository.findAll();
    }

    // Cambia el estado de la orden validando la regla de negocio del flujo
    // (no se puede pasar directo de CREADA a EN_EJECUCION sin haberla asignado antes).
    public WorkOrder updateStatus(Long id, StatusUpdateRequest request) {
        WorkOrder order = findById(id);
        WorkOrderStatus current = order.getStatus();
        WorkOrderStatus next = request.getStatus();

        if (next == WorkOrderStatus.EN_EJECUCION && current == WorkOrderStatus.CREADA) {
            throw new IllegalStateException(
                    "No se puede pasar a EN EJECUCION sin ASIGNAR la orden primero");
        }

        order.setStatus(next);
        if (request.getTechnicianName() != null) {
            order.setTechnicianName(request.getTechnicianName());
        }
        WorkOrder saved = repository.save(order);

        // Después de guardar, avisa por RabbitMQ y por Kafka (si fallan, no afectan el cambio de estado)
        eventProducer.publishStatusChange(saved);
        kafkaProducer.publish(saved);
        return saved;
    }
}