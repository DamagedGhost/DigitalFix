package com.digitalfix.ms_digitalfix_audit.service;

import com.digitalfix.ms_digitalfix_audit.dto.AuditEvent;
import com.digitalfix.ms_digitalfix_audit.model.AuditRecord;
import com.digitalfix.ms_digitalfix_audit.repository.AuditRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Lógica de negocio de auditoría. No sabe nada de Kafka. */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRecordRepository repository;

    public AuditService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    public void registrar(AuditEvent e) {
        // Validación del payload: si falta algo, no sirve reintentar (va a la DLT)
        if (e == null || e.auditId() == null || e.originalEventId() == null || e.outcome() == null) {
            throw new IllegalArgumentException(
                    "Evento de auditoría inválido: faltan auditId, originalEventId u outcome");
        }

        // Idempotencia: si Kafka entrega dos veces el mismo evento, se guarda una sola vez
        if (repository.existsByAuditId(e.auditId())) {
            log.warn("[AUDIT] Evento repetido, se ignora. auditId={}", e.auditId());
            return;
        }

        AuditRecord r = new AuditRecord();
        r.setAuditId(e.auditId());
        r.setEventTimestamp(e.timestamp());
        r.setSourceService(e.sourceService());
        r.setFlow(e.flow());
        r.setOutcome(e.outcome());
        r.setReason(e.reason() != null && e.reason().length() > 500 ? e.reason().substring(0, 500) : e.reason());
        r.setOrderId(e.orderId());
        r.setOriginalEventId(e.originalEventId());
        r.setOriginalType(e.originalType());
        r.setTraceId(e.traceId());
        r.setCorrelationId(e.correlationId());
        r.setReceivedAt(LocalDateTime.now());
        repository.save(r);
        log.info("[AUDIT] Registrado: flujo={}, resultado={}, orden={}", e.flow(), e.outcome(), e.orderId());
    }

    public List<AuditRecord> listar(String outcome, Long orderId) {
        if (outcome != null) {
            return repository.findByOutcomeOrderByIdDesc(outcome);
        }
        if (orderId != null) {
            return repository.findByOrderIdOrderByIdDesc(orderId);
        }
        return repository.findTop100ByOrderByIdDesc();
    }
}