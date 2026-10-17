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

    public void generarInforme(EventEnvelope envelope) {
        WorkOrderEventPayload p = envelope.payload();

        if (p == null || p.orderId() == null) {
            throw new UnrecoverableMessageException("Mensaje sin datos de la orden");
        }

        log.info("[REPORTE] PDF generado para la orden {} (cliente '{}', técnico '{}')",
                p.orderId(), p.clientName(), p.technicianName());
    }
}