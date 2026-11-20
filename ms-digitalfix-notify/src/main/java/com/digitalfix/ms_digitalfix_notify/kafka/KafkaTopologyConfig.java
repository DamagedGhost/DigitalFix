package com.digitalfix.ms_digitalfix_notify.kafka;

import com.digitalfix.ms_digitalfix_notify.admin.kafka.KafkaAdminClientFactory;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Define y aprovisiona al arrancar los tópicos de Kafka del sistema con parámetros
 * de administración (particiones, factor de replicación, min.insync.replicas,
 * cleanup.policy y retention.ms) ajustados a cada flujo de datos:
 *
 *  1. workorders.events      -> flujo operativo y analítica en vivo (retención 7 días).
 *  2. audit.events           -> flujo de auditoría y trazabilidad (retención 30 días).
 *  3. workorders.events.DLT  -> Dead Letter Topic de eventos de órdenes (retención 14 días).
 *  4. audit.events.DLT       -> Dead Letter Topic de eventos de auditoría (retención 14 días).
 */
@Configuration
public class KafkaTopologyConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopologyConfig.class);

    private static final String RETENTION_SIETE_DIAS = "604800000";
    private static final String RETENTION_TREINTA_DIAS = "2592000000";
    private static final String RETENTION_CATORCE_DIAS = "1209600000";

    @Bean
    public NewTopic workordersEventsTopic() {
        return TopicBuilder.name("workorders.events")
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", RETENTION_SIETE_DIAS,
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events")
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", RETENTION_TREINTA_DIAS,
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    @Bean
    public NewTopic workordersEventsDltTopic() {
        return TopicBuilder.name("workorders.events.DLT")
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", RETENTION_CATORCE_DIAS,
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    @Bean
    public NewTopic auditEventsDltTopic() {
        return TopicBuilder.name("audit.events.DLT")
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "cleanup.policy", "delete",
                        "retention.ms", RETENTION_CATORCE_DIAS,
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    /**
     * Como en el clúster Kafka KAFKA_AUTO_CREATE_TOPICS_ENABLE está en false,
     * verificamos y creamos los tópicos del sistema al arrancar si aún no existen.
     * Si Kafka no está levantado (ej. en pruebas locales), solo informa en el log sin caer.
     */
    @Bean
    public ApplicationRunner aprovisionarTopicosKafkaAlArrancar(
            KafkaAdminClientFactory factory,
            List<NewTopic> topicosDeclarados) {
        return args -> {
            try (AdminClient admin = factory.crear()) {
                int brokersActivos = admin.describeCluster().nodes().get(5, TimeUnit.SECONDS).size();
                Set<String> existentes = admin.listTopics().names().get(5, TimeUnit.SECONDS);

                List<NewTopic> faltantes = new ArrayList<>();
                for (NewTopic t : topicosDeclarados) {
                    if (!existentes.contains(t.name())) {
                        short replicasAjustadas = (short) Math.max(1, Math.min(t.replicationFactor(), brokersActivos));
                        int minIsrAjustado = Math.max(1, Math.min(2, replicasAjustadas));
                        NewTopic nuevo = new NewTopic(t.name(), t.numPartitions(), replicasAjustadas)
                                .configs(Map.of(
                                        "cleanup.policy", t.configs().getOrDefault("cleanup.policy", "delete"),
                                        "retention.ms", t.configs().getOrDefault("retention.ms", RETENTION_SIETE_DIAS),
                                        "min.insync.replicas", String.valueOf(minIsrAjustado)
                                ));
                        faltantes.add(nuevo);
                    }
                }

                if (!faltantes.isEmpty()) {
                    admin.createTopics(faltantes).all().get(8, TimeUnit.SECONDS);
                    log.info("[KAFKA-TOPOLOGY] Tópicos del sistema creados al arrancar: {}",
                            faltantes.stream().map(NewTopic::name).toList());
                } else {
                    log.info("[KAFKA-TOPOLOGY] Todos los tópicos del sistema ya existen en el clúster Kafka");
                }
            } catch (Exception e) {
                log.warn("[KAFKA-TOPOLOGY] No se pudieron verificar/crear tópicos al arrancar (Kafka no disponible aún): {}",
                        e.getMessage());
            }
        };
    }
}
