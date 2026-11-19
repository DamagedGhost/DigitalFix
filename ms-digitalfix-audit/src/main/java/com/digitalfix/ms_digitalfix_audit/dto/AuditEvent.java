package com.digitalfix.ms_digitalfix_audit.dto;

/** Mismo evento que publica notify en audit.events. */
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