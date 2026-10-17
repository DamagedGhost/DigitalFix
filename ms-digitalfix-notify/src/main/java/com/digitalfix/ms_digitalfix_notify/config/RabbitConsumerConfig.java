package com.digitalfix.ms_digitalfix_notify.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de los consumidores.
 * Hace que los mensajes JSON que llegan se conviertan solos en objetos Java.
 */
@Configuration
@EnableConfigurationProperties(ConsumerRetryProperties.class)
public class RabbitConsumerConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}