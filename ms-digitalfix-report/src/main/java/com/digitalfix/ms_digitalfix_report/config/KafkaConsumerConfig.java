package com.digitalfix.ms_digitalfix_report.config;

import com.digitalfix.ms_digitalfix_report.dto.EventEnvelope;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config del cliente Kafka de report: consumidor, manejo de errores y tópico DLT.
 */
@Configuration
@EnableConfigurationProperties(ReportKafkaProperties.class)
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, EventEnvelope> consumerFactory(ReportKafkaProperties props) {
        Map<String, Object> c = new HashMap<>();
        c.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        c.put(ConsumerConfig.GROUP_ID_CONFIG, props.groupId());
        c.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Si llega un JSON malo, no tumba al consumidor: se manda a la DLT
        c.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        c.put("spring.deserializer.value.delegate.class", JacksonJsonDeserializer.class);
        c.put("spring.json.value.default.type", EventEnvelope.class.getName());
        c.put("spring.json.use.type.headers", false);
        c.put("spring.json.trusted.packages", "com.digitalfix.ms_digitalfix_report.dto");
        // Si el group es nuevo, parte desde el primer mensaje
        c.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // El offset se confirma después de procesar bien
        c.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        c.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        c.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        return new DefaultKafkaConsumerFactory<>(c);
    }

    /** Productor que usa el recoverer para mandar los mensajes fallidos al tópico .DLT */
    @Bean
    public KafkaTemplate<String, Object> dltTemplate(ReportKafkaProperties props) {
        Map<String, Object> c = new HashMap<>();
        c.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        c.put(ProducerConfig.ACKS_CONFIG, "all");
        Map<Class<?>, Serializer<?>> tipos = new LinkedHashMap<>();
        tipos.put(byte[].class, new ByteArraySerializer());           // JSON que no se pudo leer
        tipos.put(Object.class, new JacksonJsonSerializer<Object>()); // eventos válidos que fallaron
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                c, new StringSerializer(), new DelegatingByTypeSerializer(tipos, true)));
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> dltTemplate) {
        // Tras los reintentos, el mensaje se publica en workorders.events.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltTemplate);
        // 3 reintentos, 1 segundo entre cada uno
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        // Un evento inválido no se arregla reintentando: va directo a la DLT
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        // 3 hilos consumidores = uno por partición del tópico
        factory.setConcurrency(3);
        return factory;
    }

    // Admin de Spring Kafka para crear los tópicos si faltan al arrancar (sin tumbar el servicio si Kafka aún parte)
    @Bean
    public KafkaAdmin kafkaAdmin(ReportKafkaProperties props) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }

    // Tópico principal de órdenes: 3 particiones (para concurrency=3), 3 réplicas y 7 días de retención
    @Bean
    public NewTopic workordersEventsTopic(ReportKafkaProperties props) {
        return TopicBuilder.name(props.workordersTopic())
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", "604800000", // 7 días para eventos operativos de analítica
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    // Dead Letter Topic para eventos de órdenes corruptos o que agotaron los 3 reintentos
    @Bean
    public NewTopic workordersEventsDltTopic(ReportKafkaProperties props) {
        return TopicBuilder.name(props.workordersTopic() + ".DLT")
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", "1209600000", // 14 días para diagnóstico de fallos
                        "min.insync.replicas", "2"
                ))
                .build();
    }
}