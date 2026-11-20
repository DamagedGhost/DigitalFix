package com.digitalfix.ms_digitalfix_notify.admin.kafka;

/**
 * Estado de un tópico: cuántas particiones y réplicas tiene y su configuración.
 * system = true para los tópicos del sistema, que no se pueden borrar.
 */
public record TopicInfo(
        String name,
        int partitions,
        int replicationFactor,
        String cleanupPolicy,
        Long retentionMs,
        Integer minInsyncReplicas,
        boolean system) {
}