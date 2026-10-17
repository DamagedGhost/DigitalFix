package com.digitalfix.ms_digitalfix_notify.event;

/**
 * El "sobre" común de los mensajes. Tiene la misma forma que el que arma workorders.
 */
public record EventEnvelope(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        WorkOrderEventPayload payload) {
}