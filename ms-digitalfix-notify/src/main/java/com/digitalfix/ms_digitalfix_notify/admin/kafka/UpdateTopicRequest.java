package com.digitalfix.ms_digitalfix_notify.admin.kafka;

/**
 * Lo que se puede modificar de un tópico que ya existe. Lo que venga vacío no se toca.
 * Las particiones solo se pueden aumentar, nunca bajar.
 */
public record UpdateTopicRequest(
        Integer partitions,
        Long retentionMs,
        String cleanupPolicy,
        Integer minInsyncReplicas) {
}