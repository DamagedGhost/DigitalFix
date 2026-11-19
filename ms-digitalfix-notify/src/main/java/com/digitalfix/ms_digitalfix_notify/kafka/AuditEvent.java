package com.digitalfix.ms_digitalfix_notify.kafka;

/**
 * Evento de auditoría que notify publica en Kafka (tópico audit.events).
 * Cuenta qué pasó con un mensaje que llegó por RabbitMQ.
 *
 * auditId         id único de este registro de auditoría
 * timestamp       cuándo se registró
 * sourceService   quién lo publica
 * flow            por qué cola llegó: email, dispatch o report
 * outcome         PROCESADO o ENVIADO_A_DLQ
 * reason          motivo cuando fue a la DLQ (null si salió bien)
 * orderId         orden a la que se refiere
 * originalEventId id del mensaje original de RabbitMQ (para cruzar los dos mundos)
 * originalType    tipo del evento original, por ejemplo WORKORDER_CERRADA
 * traceId         para seguirle la pista a una orden por todos los servicios
 * correlationId   agrupa todo lo de una misma orden
 */
public record AuditEvent(
        String auditId,
        String timestamp,
        String sourceService,
        String flow,
        String outcome,
        String reason,
        Long orderId,
        String originalEventId,
        String originalType,
        String traceId,
        String correlationId) {
}