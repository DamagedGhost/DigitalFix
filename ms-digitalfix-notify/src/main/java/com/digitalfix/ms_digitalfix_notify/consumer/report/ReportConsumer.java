package com.digitalfix.ms_digitalfix_notify.consumer.report;

import com.digitalfix.ms_digitalfix_notify.consumer.support.ConsumerAckSupport;
import com.digitalfix.ms_digitalfix_notify.event.EventEnvelope;
import com.digitalfix.ms_digitalfix_notify.service.ReportGenerationService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del flujo de generación de PDF.
 * El nombre de la cola se lee del application.yaml.
 */
@Component
public class ReportConsumer {

    private final ReportGenerationService reportService;
    private final ConsumerAckSupport ackSupport;

    public ReportConsumer(ReportGenerationService reportService, ConsumerAckSupport ackSupport) {
        this.reportService = reportService;
        this.ackSupport = ackSupport;
    }

    @RabbitListener(queues = "${digitalfix.rabbit.queues.report.name}")
    public void recibir(Message message, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackSupport.procesarMensaje("report", message, channel, deliveryTag, reportService::generarInforme);
    }
}