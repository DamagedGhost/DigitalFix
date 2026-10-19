package com.digitalfix.ms_digitalfix_notify.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de los consumidores.
 *
 * Se usa SimpleMessageConverter en lugar de JacksonJsonMessageConverter para evitar
 * errores fatales de conversión (ClassNotFoundException) cuando los mensajes llegan
 * con el header __TypeId__ del microservicio productor (workorders), cuya clase
 * EventEnvelope no existe en el classpath de notify.
 *
 * La deserialización JSON a EventEnvelope se hace manualmente en
 * ConsumerAckSupport.procesarMensaje(), lo cual da mayor control y tolerancia a fallos.
 */
@Configuration
@EnableConfigurationProperties(ConsumerRetryProperties.class)
public class RabbitConsumerConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new SimpleMessageConverter();
    }
}