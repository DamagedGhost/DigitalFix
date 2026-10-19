package com.digitalfix.ms_digitalfix_notify.consumer.dispatch;

import com.digitalfix.ms_digitalfix_notify.consumer.support.ConsumerAckSupport;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.service.DispatchTicketService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del flujo de tickets de despacho al técnico.
 * El nombre de la cola se lee del application.yaml.
 */
@Component
public class DispatchConsumer {

    private final DispatchTicketService dispatchService;
    private final ConsumerAckSupport ackSupport;

    public DispatchConsumer(DispatchTicketService dispatchService, ConsumerAckSupport ackSupport) {
        this.dispatchService = dispatchService;
        this.ackSupport = ackSupport;
    }

    @RabbitListener(queues = "${digitalfix.rabbit.queues.dispatch.name}")
    public void recibir(Message message, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackSupport.procesarMensaje("dispatch", message, channel, deliveryTag, dispatchService::generarTicket);
    }
}