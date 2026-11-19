package com.digitalfix.ms_digitalfix_report.dto;

public record EventEnvelope(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        WorkOrderEventPayload payload) {
}