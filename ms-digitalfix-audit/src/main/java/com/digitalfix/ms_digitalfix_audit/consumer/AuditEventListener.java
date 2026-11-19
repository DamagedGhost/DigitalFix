package com.digitalfix.ms_digitalfix_audit.consumer;

import com.digitalfix.ms_digitalfix_audit.dto.AuditEvent;
import com.digitalfix.ms_digitalfix_audit.service.AuditService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Solo recibe el mensaje de Kafka y se lo pasa al service. Nada de lógica acá. */
@Component
public class AuditEventListener {

    private final AuditService service;

    public AuditEventListener(AuditService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${digitalfix.kafka.audit-topic}", groupId = "${digitalfix.kafka.group-id}")
    public void onAuditEvent(AuditEvent event) {
        service.registrar(event);
    }
}