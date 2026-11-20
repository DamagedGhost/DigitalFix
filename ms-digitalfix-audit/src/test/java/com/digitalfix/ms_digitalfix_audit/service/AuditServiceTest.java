package com.digitalfix.ms_digitalfix_audit.service;

import com.digitalfix.ms_digitalfix_audit.dto.AuditEvent;
import com.digitalfix.ms_digitalfix_audit.model.AuditRecord;
import com.digitalfix.ms_digitalfix_audit.repository.AuditRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRecordRepository repository;

    @InjectMocks
    private AuditService service;

    private AuditEvent evento(String auditId) {
        return new AuditEvent(auditId, "2026-01-01T00:00:00Z", "ms-digitalfix-notify", "email",
                "PROCESADO", null, 1L, "evento-1", "WORKORDER_ASIGNADA", "t1", "workorder-1");
    }

    @Test
    void registrar_eventoValido_deberiaGuardar() {
        when(repository.existsByAuditId("a1")).thenReturn(false);

        service.registrar(evento("a1"));

        verify(repository).save(any(AuditRecord.class));
    }

    @Test
    void registrar_eventoRepetido_noDeberiaGuardar() {
        when(repository.existsByAuditId("a1")).thenReturn(true);

        service.registrar(evento("a1"));

        verify(repository, never()).save(any());
    }

    @Test
    void registrar_sinAuditId_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(evento(null)));
        verify(repository, never()).save(any());
    }
}