package com.digitalfix.ms_digitalfix_notify.notification;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotificationHistoryService {

    private static final int MAX_HISTORY = 50;
    private final ConcurrentLinkedDeque<NotificationRecord> history = new ConcurrentLinkedDeque<>();

    public NotificationHistoryService() {
        // Eventos iniciales de arranque para feedback inmediato en el Dashboard
        registrar("RabbitMQ Cluster inicializado y escuchando en colas q.cmd.email, q.cmd.dispatch y q.cmd.report", "SYSTEM");
        registrar("Servicio de Notificaciones y Dead Letter Queues (DLQ) activo en la red", "SYSTEM");
    }

    public void registrar(String message, String type) {
        NotificationRecord record = new NotificationRecord(
                UUID.randomUUID().toString(),
                message,
                Instant.now().toString(),
                type
        );
        history.addFirst(record);
        while (history.size() > MAX_HISTORY) {
            history.pollLast();
        }
    }

    public List<NotificationRecord> getRecentNotifications() {
        return new ArrayList<>(history);
    }
}
