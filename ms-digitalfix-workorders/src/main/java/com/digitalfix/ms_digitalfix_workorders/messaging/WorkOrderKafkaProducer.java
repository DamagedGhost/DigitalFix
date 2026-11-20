package com.digitalfix.ms_digitalfix_workorders.messaging;

import com.digitalfix.ms_digitalfix_workorders.config.KafkaProducerProperties;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor de eventos hacia Kafka (tópico workorders.events).
 * Publica un evento cada vez que una orden se crea o cambia de estado.
 * Va aparte del productor de RabbitMQ: no se tocan entre sí.
 */
@Component
public class WorkOrderKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderKafkaProducer.class);

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final KafkaProducerProperties props;

    public WorkOrderKafkaProducer(KafkaTemplate<String, EventEnvelope> kafkaTemplate,
                                  KafkaProducerProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publish(WorkOrder order) {
        try {
            String key = "workorder-" + order.getId();

            EventEnvelope envelope = new EventEnvelope(
                    "WORKORDER_" + order.getStatus().name(),
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    UUID.randomUUID().toString(),
                    key,
                    new WorkOrderEventPayload(
                            order.getId(),
                            order.getClientName(),
                            order.getTechnicianName(),
                            order.getStatus().name()));

            // La key hace que todos los eventos de una misma orden caigan en la misma partición,
            // así se leen en el orden en que pasaron
            kafkaTemplate.send(props.topic(), key, envelope).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("No se pudo enviar evento a Kafka (orden={}): {}", order.getId(), ex.getMessage());
                } else {
                    log.info("Evento enviado a Kafka: topic={}, partición={}, offset={}, orden={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            order.getId());
                }
            });
        } catch (Exception e) {
            // Si Kafka falla, el cambio de estado igual queda guardado
            log.error("Error armando o enviando evento a Kafka (orden={}): {}", order.getId(), e.getMessage());
        }
    }
}