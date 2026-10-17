package com.digitalfix.ms_digitalfix_notify.service;

import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.event.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_notify.exception.UnrecoverableMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ticket de despacho al técnico (simulado: solo escribe en el log).
 */
@Service
public class DispatchTicketService {

    private static final Logger log = LoggerFactory.getLogger(DispatchTicketService.class);

    public void generarTicket(EventEnvelope envelope) {
        WorkOrderEventPayload p = envelope.payload();

        if (p == null) {
            throw new UnrecoverableMessageException("Mensaje sin datos de la orden");
        }
        // No se puede despachar a nadie si la orden no tiene técnico asignado
        if (p.technicianName() == null || p.technicianName().isBlank()) {
            throw new UnrecoverableMessageException(
                    "No se puede generar el ticket: la orden " + p.orderId() + " no tiene técnico");
        }

        log.info("[DESPACHO] Ticket generado para el técnico '{}' por la orden {} (cliente '{}')",
                p.technicianName(), p.orderId(), p.clientName());
    }
}