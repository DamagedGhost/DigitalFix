package com.digitalfix.ms_digitalfix_report.service;

import com.digitalfix.ms_digitalfix_report.dto.EventEnvelope;
import com.digitalfix.ms_digitalfix_report.dto.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_report.model.OrderSnapshot;
import com.digitalfix.ms_digitalfix_report.repository.OrderSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de analítica en streaming sobre las órdenes.
 * En vez de consultar a ms-workorders cada vez que el Admin abre Reportería, mantengo una vista
 * materializada propia (tabla order_snapshots) que se actualiza sola con cada evento de Kafka.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final OrderSnapshotRepository repository;

    public ReportService(OrderSnapshotRepository repository) {
        this.repository = repository;
    }

    /**
     * Aplica un evento de orden sobre el snapshot local (inserta si es nueva, actualiza si ya existe).
     * - Si el payload viene incompleto tiro IllegalArgumentException para que Kafka lo mande directo a la DLT.
     * - Si ya procesé ese eventId, lo ignoro para garantizar idempotencia ante reentregas.
     */
    @Transactional
    public void aplicarEvento(EventEnvelope evento) {
        if (evento == null || evento.eventId() == null || evento.payload() == null
                || evento.payload().orderId() == null || evento.payload().status() == null) {
            throw new IllegalArgumentException("Evento inválido: faltan eventId, orderId o status");
        }

        WorkOrderEventPayload p = evento.payload();
        OrderSnapshot snapshot = repository.findById(p.orderId()).orElseGet(OrderSnapshot::new);

        if (evento.eventId().equals(snapshot.getLastEventId())) {
            log.warn("[REPORT] Evento repetido, se ignora. eventId={}", evento.eventId());
            return;
        }

        snapshot.setOrderId(p.orderId());
        snapshot.setClientName(p.clientName());
        // Solo piso el técnico si el evento trae uno; así un cambio posterior sin técnico (ej. cierre) no borra al asignado
        if (p.technicianName() != null) {
            snapshot.setTechnicianName(p.technicianName());
        }
        snapshot.setStatus(p.status());
        snapshot.setLastEventId(evento.eventId());
        snapshot.setUpdatedAt(LocalDateTime.now());
        repository.save(snapshot);

        log.info("[REPORT] Orden {} actualizada: estado={}, técnico={}",
                p.orderId(), p.status(), snapshot.getTechnicianName());
    }

    // Conteo agrupado por estado (CREADA, ASIGNADA, CERRADA, etc.) para las tarjetas y gráficos del front
    @Transactional(readOnly = true)
    public Map<String, Long> porEstado() {
        return aMapa(repository.contarPorEstado());
    }

    // Ranking de carga por técnico ordenado de mayor a menor
    @Transactional(readOnly = true)
    public Map<String, Long> porTecnico() {
        return aMapa(repository.contarPorTecnico());
    }

    @Transactional(readOnly = true)
    public long totalOrdenes() {
        return repository.count();
    }

    // Convierte la proyección JPA (lista de clave/total) a un Map que conserva el orden de la query SQL
    private Map<String, Long> aMapa(List<OrderSnapshotRepository.Conteo> filas) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (OrderSnapshotRepository.Conteo fila : filas) {
            mapa.put(fila.getClave(), fila.getTotal());
        }
        return mapa;
    }
}