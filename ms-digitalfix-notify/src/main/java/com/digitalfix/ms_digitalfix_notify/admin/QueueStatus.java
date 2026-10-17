package com.digitalfix.ms_digitalfix_notify.admin;

/**
 * Estado de una cola: cuántos mensajes tiene esperando y cuántos consumidores la están leyendo.
 */
public record QueueStatus(String flow, String type, String queue, long messages, int consumers) {
}