package com.digitalfix.ms_digitalfix_report.dto;

public record WorkOrderEventPayload(
        Long orderId,
        String clientName,
        String technicianName,
        String status) {
}