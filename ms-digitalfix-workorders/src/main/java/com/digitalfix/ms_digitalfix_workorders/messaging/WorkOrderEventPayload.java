package com.digitalfix.ms_digitalfix_workorders.messaging;

/**
 * Los datos de la orden que viajan dentro del sobre.
 */
public record WorkOrderEventPayload(
        Long orderId,
        String clientName,
        String technicianName,
        String status) {
}