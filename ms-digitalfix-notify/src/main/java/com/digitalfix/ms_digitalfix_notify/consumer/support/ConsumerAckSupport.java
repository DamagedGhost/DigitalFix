package com.digitalfix.ms_digitalfix_notify.consumer.support;

import com.digitalfix.ms_digitalfix_notify.config.ConsumerRetryProperties;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.exception.RecoverableMessageException;
import com.digitalfix.ms_digitalfix_notify.exception.UnrecoverableMessageException;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Decide qué hacer con cada mensaje. Las reglas son siempre las mismas:
 *
 *  1. Mensaje repetido (mismo eventId)       -> ACK y se ignora (idempotencia).
 *  2. Se procesa bien                        -> ACK.
 *  3. Error temporal                         -> se reintenta (max-attempts veces);
 *                                               si sigue fallando -> NACK sin requeue -> DLQ.
 *  4. Error sin arreglo o error inesperado   -> NACK sin requeue -> DLQ, sin reintentos.
 *
 * Todo mensaje que va a la DLQ queda anotado en el log con el motivo.
 */
@Component
public class ConsumerAckSupport {

    private static final Logger log = LoggerFactory.getLogger(ConsumerAckSupport.class);

    private final ConsumerRetryProperties retry;

    // eventIds ya procesados (en memoria, alcanza para esta evaluación)
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public ConsumerAckSupport(ConsumerRetryProperties retry) {
        this.retry = retry;
    }

    public void procesar(String flujo, EventEnvelope envelope, Channel channel,
                         long deliveryTag, Consumer<EventEnvelope> accion) throws IOException {

        // Sobre vacío o sin id: no se puede procesar ni deduplicar
        if (envelope == null || envelope.eventId() == null) {
            log.error("[DLQ][{}] Mensaje inválido (sin sobre o sin eventId): {}", flujo, envelope);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // 1. Idempotencia
        if (procesados.contains(envelope.eventId())) {
            log.warn("[{}] Mensaje repetido, se ignora. eventId={}", flujo, envelope.eventId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            ejecutarConReintentos(flujo, envelope, accion);
            procesados.add(envelope.eventId());
            // 2. Todo bien
            channel.basicAck(deliveryTag, false);

        } catch (UnrecoverableMessageException e) {
            // 4. Error sin arreglo
            log.error("[DLQ][{}] Error sin arreglo, se envía a la DLQ. Motivo: {} | Mensaje: {}",
                    flujo, e.getMessage(), envelope);
            channel.basicNack(deliveryTag, false, false);

        } catch (RecoverableMessageException e) {
            // 3. Se acabaron los reintentos
            log.error("[DLQ][{}] Se agotaron los {} intentos, se envía a la DLQ. Motivo: {} | Mensaje: {}",
                    flujo, retry.maxAttempts(), e.getMessage(), envelope);
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // 4. Error inesperado: no se reintenta para no quedar en un ciclo infinito
            log.error("[DLQ][{}] Error inesperado, se envía a la DLQ. Motivo: {} | Mensaje: {}",
                    flujo, e.getMessage(), envelope, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // Reintenta solo los errores temporales; los demás errores salen de inmediato
    private void ejecutarConReintentos(String flujo, EventEnvelope envelope, Consumer<EventEnvelope> accion) {
        int intento = 1;
        while (true) {
            try {
                accion.accept(envelope);
                return;
            } catch (RecoverableMessageException e) {
                if (intento >= retry.maxAttempts()) {
                    throw e;
                }
                log.warn("[{}] Error temporal ({}), reintento {}/{} en {} ms",
                        flujo, e.getMessage(), intento, retry.maxAttempts(), retry.backoffMs());
                esperar();
                intento++;
            }
        }
    }

    private void esperar() {
        try {
            Thread.sleep(retry.backoffMs());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RecoverableMessageException("Reintento interrumpido");
        }
    }
}