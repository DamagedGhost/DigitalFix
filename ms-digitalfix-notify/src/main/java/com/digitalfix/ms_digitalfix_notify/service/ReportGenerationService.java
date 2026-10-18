package com.digitalfix.ms_digitalfix_notify.service;

import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.event.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_notify.exception.UnrecoverableMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generación del PDF de cierre (simulado: solo escribe en el log).
 */
@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);
    private final com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService historyService;

    public ReportGenerationService() {
        this(new com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService());
    }

    public ReportGenerationService(com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService historyService) {
        this.historyService = historyService != null ? historyService : new com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService();
    }

    public void generarInforme(EventEnvelope envelope) {
        WorkOrderEventPayload p = envelope.payload();

        if (p == null || p.orderId() == null) {
            throw new UnrecoverableMessageException("Mensaje sin datos de la orden");
        }

        log.info("[REPORTE] PDF generado para la orden {} (cliente '{}', técnico '{}')",
                p.orderId(), p.clientName(), p.technicianName());
        historyService.registrar(
                "Informe de cierre y métricas generado para la orden #" + p.orderId() + " (Cliente: " + p.clientName() + ")",
                "REPORT"
        );
    }
}