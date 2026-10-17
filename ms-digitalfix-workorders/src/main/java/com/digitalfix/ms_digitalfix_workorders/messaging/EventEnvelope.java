package com.digitalfix.ms_digitalfix_workorders.messaging;

/**
 * El "sobre" común de todos los mensajes (lo pide el caso).
 * type: qué pasó, eventId: id único del mensaje, traceId/correlationId: para seguirle la pista.
 */
public record EventEnvelope(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        WorkOrderEventPayload payload) {
}