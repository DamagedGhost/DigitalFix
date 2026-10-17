package com.digitalfix.ms_digitalfix_notify.admin;

/**
 * Datos para crear una cola.
 * exchange: "direct" o "topic" (si viene vacío se usa "direct").
 */
public record CreateQueueRequest(String name, String exchange, String routingKey) {
}