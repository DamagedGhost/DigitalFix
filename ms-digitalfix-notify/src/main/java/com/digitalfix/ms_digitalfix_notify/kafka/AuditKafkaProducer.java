package com.digitalfix.ms_digitalfix_notify.kafka;

import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica en Kafka (audit.events) qué pasó con cada mensaje que notify recibe por RabbitMQ.
 * Nunca lanza excepciones: si Kafka falla, el procesamiento de RabbitMQ sigue igual.
 */
@Component
public class AuditKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaProducer.class);
    private static final String SERVICIO = "ms-digitalfix-notify";

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final KafkaProducerProperties props;

    public AuditKafkaProducer(KafkaTemplate<String, AuditEvent> kafkaTemplate,
                              KafkaProducerProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publicar(String flujo, EventEnvelope envelope, String resultado, String motivo) {
        try {
            if (envelope == null) {
                return;
            }
            Long orderId = envelope.payload() != null ? envelope.payload().orderId() : null;

            // La key agrupa todo lo de una misma orden en la misma partición (se lee en orden)
            String key = envelope.correlationId() != null ? envelope.correlationId() : envelope.eventId();

            AuditEvent evento = new AuditEvent(
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    SERVICIO,
                    flujo,
                    resultado,
                    motivo,
                    orderId,
                    envelope.eventId(),
                    envelope.type(),
                    envelope.traceId(),
                    envelope.correlationId());

            kafkaTemplate.send(props.auditTopic(), key, evento).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[AUDIT] No se pudo enviar a Kafka (flujo={}, eventId={}): {}",
                            flujo, envelope.eventId(), ex.getMessage());
                } else {
                    log.info("[AUDIT] Evento enviado a Kafka: topic={}, partición={}, offset={}, flujo={}, resultado={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            flujo, resultado);
                }
            });
        } catch (Exception e) {
            // Si Kafka falla, RabbitMQ sigue funcionando
            log.error("[AUDIT] Error armando o enviando evento a Kafka (flujo={}): {}", flujo, e.getMessage());
        }
    }
}