package com.digitalfix.ms_digitalfix_notify.admin.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas de las validaciones del admin de Kafka. Fallan antes de abrir la conexión,
 * así que no necesitan Kafka (el factory es un mock y verificamos que nunca se usa).
 */
@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private KafkaAdminClientFactory factory;

    @InjectMocks
    private KafkaAdminService service;

    @Test
    void crear_sinCuerpo_deberiaDar400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crear(null));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(factory, never()).crear();
    }

    @Test
    void crear_nombreConEspacios_deberiaDar400() {
        CreateTopicRequest request = new CreateTopicRequest("nombre malo", 3, 3, null, null, null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(factory, never()).crear();
    }

    @Test
    void crear_particionesEnCero_deberiaDar400() {
        CreateTopicRequest request = new CreateTopicRequest("demo.eventos", 0, 3, null, null, null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(factory, never()).crear();
    }

    @Test
    void crear_politicaInvalida_deberiaDar400() {
        CreateTopicRequest request = new CreateTopicRequest("demo.eventos", 3, 3, null, "borrar", null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(factory, never()).crear();
    }

    @Test
    void modificar_sinCambios_deberiaDar400() {
        UpdateTopicRequest request = new UpdateTopicRequest(null, null, null, null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.modificar("demo.eventos", request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(factory, never()).crear();
    }

    @Test
    void eliminar_topicoDelSistema_deberiaDar409() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.eliminar("workorders.events"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(factory, never()).crear();
    }
}