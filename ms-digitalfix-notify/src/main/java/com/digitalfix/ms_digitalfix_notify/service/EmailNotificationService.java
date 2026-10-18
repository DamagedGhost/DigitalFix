package com.digitalfix.ms_digitalfix_notify.service;

import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.event.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_notify.exception.RecoverableMessageException;
import com.digitalfix.ms_digitalfix_notify.exception.UnrecoverableMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envío de email/push al cliente (simulado: solo escribe en el log).
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private final com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService historyService;

    public EmailNotificationService() {
        this(new com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService());
    }

    public EmailNotificationService(com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService historyService) {
        this.historyService = historyService != null ? historyService : new com.digitalfix.ms_digitalfix_notify.notification.NotificationHistoryService();
    }

    public void enviarEmail(EventEnvelope envelope) {
        WorkOrderEventPayload p = envelope.payload();

        // Sin datos del cliente no hay a quién escribirle: no tiene arreglo
        if (p == null || p.clientName() == null || p.clientName().isBlank()) {
            throw new UnrecoverableMessageException("Mensaje sin datos del cliente");
        }

        // Para la demo: un cliente con "FALLA" en el nombre simula que el servidor de correo está caído
        if (p.clientName().toUpperCase().contains("FALLA")) {
            throw new RecoverableMessageException("Servidor de correo no disponible");
        }

        log.info("[EMAIL] Aviso enviado al cliente '{}' por la orden {} (estado {})",
                p.clientName(), p.orderId(), p.status());
        historyService.registrar(
                "Aviso de email enviado a '" + p.clientName() + "' por la orden #" + p.orderId() + " (estado " + p.status() + ")",
                "EMAIL"
        );
    }
}