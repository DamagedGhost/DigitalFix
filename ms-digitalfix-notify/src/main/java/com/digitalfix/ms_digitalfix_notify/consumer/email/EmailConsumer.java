package com.digitalfix.ms_digitalfix_notify.consumer.email;

import com.digitalfix.ms_digitalfix_notify.consumer.support.ConsumerAckSupport;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.service.EmailNotificationService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del flujo de emails al cliente.
 * El nombre de la cola se lee del application.yaml.
 */
@Component
public class EmailConsumer {

    private final EmailNotificationService emailService;
    private final ConsumerAckSupport ackSupport;

    public EmailConsumer(EmailNotificationService emailService, ConsumerAckSupport ackSupport) {
        this.emailService = emailService;
        this.ackSupport = ackSupport;
    }

    @RabbitListener(queues = "${digitalfix.rabbit.queues.email.name}")
    public void recibir(EventEnvelope envelope, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackSupport.procesar("email", envelope, channel, deliveryTag, emailService::enviarEmail);
    }
}