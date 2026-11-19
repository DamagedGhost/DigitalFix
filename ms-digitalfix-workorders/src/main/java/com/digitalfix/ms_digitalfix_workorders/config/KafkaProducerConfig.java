package com.digitalfix.ms_digitalfix_workorders.config;

import com.digitalfix.ms_digitalfix_workorders.messaging.EventEnvelope;
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
 * Toda la config del cliente Kafka (productor) en un solo lugar.
 */
@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, EventEnvelope> producerFactory(KafkaProducerProperties props) {
        Map<String, Object> config = new HashMap<>();

        // A qué brokers se conecta
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());

        // Cómo convierte la key (texto) y el valor (EventEnvelope -> JSON)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        // No mandar el nombre de la clase Java en un header, así los consumers de otros
        // microservicios (con su propio paquete) pueden leer el JSON sin problemas
        config.put("spring.json.add.type.headers", false);

        // Durabilidad: el mensaje se confirma cuando lo tienen todas las réplicas en sincronía
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Idempotencia: si hay un reintento, Kafka no deja el mensaje duplicado
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Tiempos: si Kafka está caído, que falle rápido y no deje colgada la petición HTTP
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);

        // Junta mensajes por 10 ms antes de enviar (mejor rendimiento)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, EventEnvelope> kafkaTemplate(ProducerFactory<String, EventEnvelope> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}