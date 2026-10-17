package com.digitalfix.ms_digitalfix_notify.event;

/**
 * Los datos de la orden que vienen dentro del sobre.
 */
public record WorkOrderEventPayload(
        Long orderId,
        String clientName,
        String technicianName,
        String status) {
}