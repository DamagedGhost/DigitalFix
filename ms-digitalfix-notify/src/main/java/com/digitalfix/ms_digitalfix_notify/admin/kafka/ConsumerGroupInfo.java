package com.digitalfix.ms_digitalfix_notify.admin.kafka;

import java.util.List;

/**
 * Estado de un consumer group: cuántos consumidores tiene conectados, cuántos
 * mensajes le faltan por leer en total (totalLag) y el detalle por partición.
 */
public record ConsumerGroupInfo(
        String groupId,
        String state,
        int members,
        long totalLag,
        List<GroupOffsetInfo> offsets) {
}