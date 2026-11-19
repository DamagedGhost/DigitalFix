package com.digitalfix.ms_digitalfix_notify.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Toda la config del cliente Kafka (productor) de notify en un solo lugar.
 */
@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, AuditEvent> auditProducerFactory(KafkaProducerProperties props) {
        Map<String, Object> config = new HashMap<>();

        // A qué brokers se conecta
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());

        // Cómo convierte la key (texto) y el valor (AuditEvent -> JSON)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        // Sin nombre de clase Java en headers: el consumer (ms-audit) lee el JSON con su propio DTO
        config.put("spring.json.add.type.headers", false);

        // Durabilidad: se confirma cuando lo tienen todas las réplicas en sincronía
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Idempotencia: un reintento no deja el mensaje duplicado
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Tiempos cortos: si Kafka está caído, no debe frenar a los consumidores de RabbitMQ
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);

        // Junta mensajes por 10 ms antes de enviar
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AuditEvent> auditKafkaTemplate(ProducerFactory<String, AuditEvent> auditProducerFactory) {
        return new KafkaTemplate<>(auditProducerFactory);
    }
}