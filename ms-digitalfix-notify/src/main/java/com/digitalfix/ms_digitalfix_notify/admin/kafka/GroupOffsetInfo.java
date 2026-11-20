package com.digitalfix.ms_digitalfix_notify.admin.kafka;

/**
 * Una partición vista por un consumer group.
 * currentOffset: hasta dónde leyó el grupo. endOffset: hasta dónde hay mensajes.
 * lag = endOffset - currentOffset (mensajes pendientes de leer).
 */
public record GroupOffsetInfo(
        String topic,
        int partition,
        long currentOffset,
        long endOffset,
        long lag) {
}