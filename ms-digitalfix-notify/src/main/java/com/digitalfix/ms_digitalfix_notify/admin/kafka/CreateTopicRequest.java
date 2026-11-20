package com.digitalfix.ms_digitalfix_notify.admin.kafka;

/**
 * Datos para crear un tópico.
 * Si partitions o replicationFactor vienen vacíos se usa 3 (lo que pide el caso).
 * cleanupPolicy: "delete" o "compact". retentionMs: cuánto se guardan los mensajes (-1 = para siempre).
 */
public record CreateTopicRequest(
        String name,
        Integer partitions,
        Integer replicationFactor,
        Long retentionMs,
        String cleanupPolicy,
        Integer minInsyncReplicas) {
}