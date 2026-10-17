package com.digitalfix.ms_digitalfix_notify.consumer.support;

import com.digitalfix.ms_digitalfix_notify.config.ConsumerRetryProperties;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.event.WorkOrderEventPayload;
import com.digitalfix.ms_digitalfix_notify.exception.RecoverableMessageException;
import com.digitalfix.ms_digitalfix_notify.exception.UnrecoverableMessageException;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pruebas de las reglas de ACK/NACK: camino feliz, error temporal con reintentos,
 * error sin arreglo e idempotencia.
 */
@ExtendWith(MockitoExtension.class)
class ConsumerAckSupportTest {

    private static final long TAG = 1L;

    @Mock
    private Channel channel;

    private ConsumerAckSupport support;
    private EventEnvelope envelope;

    @BeforeEach
    void setUp() {
        // 3 intentos y 1 ms de espera para que el test sea rápido
        support = new ConsumerAckSupport(new ConsumerRetryProperties(3, 1));
        envelope = new EventEnvelope("WORKORDER_ASIGNADA", "evento-1", "2026-01-01T00:00:00Z",
                "trace-1", "workorder-1",
                new WorkOrderEventPayload(1L, "Maria Perez", "Pedro", "ASIGNADA"));
    }

    @SuppressWarnings("unchecked")
    private Consumer<EventEnvelope> accion() {
        return mock(Consumer.class);
    }

    @Test
    void procesar_todoBien_deberiaHacerAck() throws IOException {
        Consumer<EventEnvelope> accion = accion();

        support.procesar("email", envelope, channel, TAG, accion);

        verify(accion, times(1)).accept(envelope);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }

    @Test
    void procesar_errorSinArreglo_deberiaHacerNackSinReintentos() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        doThrow(new UnrecoverableMessageException("sin técnico")).when(accion).accept(any());

        support.procesar("dispatch", envelope, channel, TAG, accion);

        verify(accion, times(1)).accept(envelope);   // un solo intento
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(TAG, false);
    }

    @Test
    void procesar_errorTemporalPersistente_deberiaReintentar3VecesYHacerNack() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        doThrow(new RecoverableMessageException("correo caído")).when(accion).accept(any());

        support.procesar("email", envelope, channel, TAG, accion);

        verify(accion, times(3)).accept(envelope);   // los 3 intentos
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(TAG, false);
    }

    @Test
    void procesar_errorTemporalQueSeRecupera_deberiaHacerAck() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        doThrow(new RecoverableMessageException("correo caído"))
                .doNothing()
                .when(accion).accept(any());

        support.procesar("email", envelope, channel, TAG, accion);

        verify(accion, times(2)).accept(envelope);   // falla una vez y luego funciona
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }

    @Test
    void procesar_errorInesperado_deberiaHacerNackSinReintentos() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        doThrow(new IllegalStateException("algo raro")).when(accion).accept(any());

        support.procesar("report", envelope, channel, TAG, accion);

        verify(accion, times(1)).accept(envelope);
        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void procesar_mensajeRepetido_deberiaIgnorarloConAck() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        doNothing().when(accion).accept(any());

        support.procesar("email", envelope, channel, TAG, accion);
        support.procesar("email", envelope, channel, 2L, accion); // mismo eventId

        verify(accion, times(1)).accept(envelope);   // el segundo no se procesa
        verify(channel).basicAck(TAG, false);
        verify(channel).basicAck(2L, false);
    }

    @Test
    void procesar_sobreSinEventId_deberiaHacerNack() throws IOException {
        Consumer<EventEnvelope> accion = accion();
        EventEnvelope sinId = new EventEnvelope("X", null, null, null, null, null);

        support.procesar("email", sinId, channel, TAG, accion);

        verify(accion, never()).accept(any());
        verify(channel).basicNack(TAG, false, false);
    }
}