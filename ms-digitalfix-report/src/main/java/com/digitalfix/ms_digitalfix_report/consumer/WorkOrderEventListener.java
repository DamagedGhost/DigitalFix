package com.digitalfix.ms_digitalfix_report.consumer;

import com.digitalfix.ms_digitalfix_report.dto.EventEnvelope;
import com.digitalfix.ms_digitalfix_report.service.ReportService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del tópico workorders.events (grupo report-group).
 * Solo recibe el evento y se lo pasa a ReportService para mantener separada la mensajería de la lógica de negocio.
 */
@Component
public class WorkOrderEventListener {

    private final ReportService service;

    public WorkOrderEventListener(ReportService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${digitalfix.kafka.workorders-topic}", groupId = "${digitalfix.kafka.group-id}")
    public void onWorkOrderEvent(EventEnvelope event) {
        service.aplicarEvento(event);
    }
}