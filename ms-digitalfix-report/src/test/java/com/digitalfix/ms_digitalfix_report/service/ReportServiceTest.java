package com.digitalfix.ms_digitalfix_report.service;

import com.digitalfix.ms_digitalfix_report.dto.EventEnvelope;
import com.digitalfix.ms_digitalfix_report.dto.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_report.model.OrderSnapshot;
import com.digitalfix.ms_digitalfix_report.repository.OrderSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderSnapshotRepository repository;

    @InjectMocks
    private ReportService service;

    private EventEnvelope evento(String eventId, Long orderId, String tecnico, String status) {
        return new EventEnvelope("WORKORDER_" + status, eventId, "2026-01-01T00:00:00Z",
                "t1", "workorder-" + orderId,
                new WorkOrderEventPayload(orderId, "Maria Perez", tecnico, status));
    }

    private OrderSnapshotRepository.Conteo conteo(String clave, long total) {
        OrderSnapshotRepository.Conteo c = mock(OrderSnapshotRepository.Conteo.class);
        when(c.getClave()).thenReturn(clave);
        when(c.getTotal()).thenReturn(total);
        return c;
    }

    @Test
    void aplicarEvento_ordenNueva_deberiaGuardarSnapshot() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        service.aplicarEvento(evento("e1", 1L, "Pedro", "ASIGNADA"));

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(repository).save(captor.capture());
        OrderSnapshot guardado = captor.getValue();
        assertEquals(1L, guardado.getOrderId().longValue());
        assertEquals("ASIGNADA", guardado.getStatus());
        assertEquals("Pedro", guardado.getTechnicianName());
        assertEquals("e1", guardado.getLastEventId());
    }

    @Test
    void aplicarEvento_eventoRepetido_noDeberiaGuardar() {
        OrderSnapshot existente = new OrderSnapshot();
        existente.setOrderId(1L);
        existente.setLastEventId("e1");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.aplicarEvento(evento("e1", 1L, "Pedro", "CERRADA"));

        verify(repository, never()).save(any());
    }

    @Test
    void aplicarEvento_sinTecnico_noDeberiaBorrarElQueYaTenia() {
        OrderSnapshot existente = new OrderSnapshot();
        existente.setOrderId(1L);
        existente.setTechnicianName("Pedro");
        existente.setLastEventId("e1");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.aplicarEvento(evento("e2", 1L, null, "EN_EJECUCION"));

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(repository).save(captor.capture());
        assertEquals("Pedro", captor.getValue().getTechnicianName());
        assertEquals("EN_EJECUCION", captor.getValue().getStatus());
    }

    @Test
    void aplicarEvento_sinOrderId_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarEvento(evento("e1", null, "Pedro", "ASIGNADA")));

        verify(repository, never()).save(any());
    }

    @Test
    void porEstado_deberiaArmarMapaConLosConteos() {
        List<OrderSnapshotRepository.Conteo> filas =
                List.of(conteo("ASIGNADA", 2L), conteo("CERRADA", 1L));
        when(repository.contarPorEstado()).thenReturn(filas);

        Map<String, Long> resultado = service.porEstado();

        assertEquals(2, resultado.size());
        assertEquals(2L, resultado.get("ASIGNADA").longValue());
        assertEquals(1L, resultado.get("CERRADA").longValue());
    }
}