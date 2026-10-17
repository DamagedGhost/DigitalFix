package com.digitalfix.ms_digitalfix_workorders.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería de workorders.
 * Hace que los mensajes viajen como JSON (legible en el panel de RabbitMQ).
 */
@Configuration
@EnableConfigurationProperties(RabbitProducerProperties.class)
public class RabbitProducerConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}