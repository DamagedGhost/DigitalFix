package com.digitalfix.ms_digitalfix_notify.admin.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Administración de Kafka: tópicos (crear, listar, modificar, borrar) y consumer groups
 * (estado, offsets y lag). Usa el AdminClient de Kafka.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    // Tópicos del sistema: no se pueden borrar
    private static final Set<String> TOPICOS_SISTEMA = Set.of("workorders.events", "audit.events");
    private static final Set<String> POLITICAS = Set.of("delete", "compact", "delete,compact", "compact,delete");
    // Solo letras, números, punto, guion y guion bajo (sin espacios)
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int LARGO_MAXIMO = 100;
    private static final int MAX_PARTICIONES = 30;
    private static final int MAX_REPLICAS = 5;
    private static final int TIMEOUT_SEGUNDOS = 8;

    private final KafkaAdminClientFactory factory;

    public KafkaAdminService(KafkaAdminClientFactory factory) {
        this.factory = factory;
    }

    // ---------- Tópicos ----------

    public List<TopicInfo> listarTopicos() {
        try (AdminClient admin = factory.crear()) {
            Set<String> nombres = new TreeSet<>(esperar(admin.listTopics().names()));
            if (nombres.isEmpty()) {
                return List.of();
            }
            return armarTopicos(admin, nombres);
        }
    }

    public TopicInfo consultarTopico(String nombre) {
        try (AdminClient admin = factory.crear()) {
            return armarTopicos(admin, List.of(nombre)).get(0);
        }
    }

    public TopicInfo crear(CreateTopicRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el cuerpo de la petición");
        }
        String nombre = validarNombre(request.name());
        int particiones = request.partitions() == null ? 3 : request.partitions();
        int replicas = request.replicationFactor() == null ? 3 : request.replicationFactor();
        if (particiones < 1 || particiones > MAX_PARTICIONES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las particiones deben estar entre 1 y " + MAX_PARTICIONES);
        }
        if (replicas < 1 || replicas > MAX_REPLICAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El factor de replicación debe estar entre 1 y " + MAX_REPLICAS);
        }

        Map<String, String> configs = new HashMap<>();
        if (request.retentionMs() != null) {
            validarRetencion(request.retentionMs());
            configs.put("retention.ms", String.valueOf(request.retentionMs()));
        }
        if (request.cleanupPolicy() != null) {
            validarPolitica(request.cleanupPolicy());
            configs.put("cleanup.policy", request.cleanupPolicy());
        }
        if (request.minInsyncReplicas() != null) {
            if (request.minInsyncReplicas() < 1 || request.minInsyncReplicas() > replicas) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "min.insync.replicas debe estar entre 1 y el factor de replicación (" + replicas + ")");
            }
            configs.put("min.insync.replicas", String.valueOf(request.minInsyncReplicas()));
        }

        try (AdminClient admin = factory.crear()) {
            NewTopic topico = new NewTopic(nombre, particiones, (short) replicas).configs(configs);
            esperar(admin.createTopics(List.of(topico)).all());
            log.info("[ADMIN-KAFKA] Tópico '{}' creado: {} particiones, {} réplicas, configs={}",
                    nombre, particiones, replicas, configs);
            return new TopicInfo(nombre, particiones, replicas,
                    configs.getOrDefault("cleanup.policy", "delete"),
                    request.retentionMs(), request.minInsyncReplicas(), false);
        }
    }

    public TopicInfo modificar(String nombre, UpdateTopicRequest request) {
        if (request == null || (request.partitions() == null && request.retentionMs() == null
                && request.cleanupPolicy() == null && request.minInsyncReplicas() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay nada que modificar");
        }
        if (request.retentionMs() != null) {
            validarRetencion(request.retentionMs());
        }
        if (request.cleanupPolicy() != null) {
            validarPolitica(request.cleanupPolicy());
        }
        if (request.partitions() != null && request.partitions() > MAX_PARTICIONES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las particiones no pueden superar " + MAX_PARTICIONES);
        }

        try (AdminClient admin = factory.crear()) {
            TopicDescription actual = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
            if (request.partitions() != null && request.partitions() <= actual.partitions().size()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Las particiones solo se pueden aumentar (hoy tiene " + actual.partitions().size() + ")");
            }

            List<AlterConfigOp> cambios = new ArrayList<>();
            if (request.retentionMs() != null) {
                cambios.add(operacion("retention.ms", String.valueOf(request.retentionMs())));
            }
            if (request.cleanupPolicy() != null) {
                cambios.add(operacion("cleanup.policy", request.cleanupPolicy()));
            }
            if (request.minInsyncReplicas() != null) {
                cambios.add(operacion("min.insync.replicas", String.valueOf(request.minInsyncReplicas())));
            }
            if (!cambios.isEmpty()) {
                Map<ConfigResource, Collection<AlterConfigOp>> pedido =
                        Map.of(new ConfigResource(ConfigResource.Type.TOPIC, nombre), cambios);
                esperar(admin.incrementalAlterConfigs(pedido).all());
            }
            if (request.partitions() != null) {
                esperar(admin.createPartitions(
                        Map.of(nombre, NewPartitions.increaseTo(request.partitions()))).all());
            }
            log.warn("[ADMIN-KAFKA] Tópico '{}' modificado: {}", nombre, request);
            return armarTopicos(admin, List.of(nombre)).get(0);
        }
    }

    public void eliminar(String nombre) {
        if (TOPICOS_SISTEMA.contains(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede borrar '" + nombre + "': es un tópico del sistema");
        }
        try (AdminClient admin = factory.crear()) {
            esperar(admin.deleteTopics(List.of(nombre)).all());
            log.warn("[ADMIN-KAFKA] Tópico '{}' borrado", nombre);
        }
    }

    // ---------- Consumer groups ----------

    public List<ConsumerGroupInfo> listarGrupos() {
        try (AdminClient admin = factory.crear()) {
            List<String> ids = esperar(admin.listConsumerGroups().all()).stream()
                    .map(ConsumerGroupListing::groupId)
                    .sorted()
                    .toList();
            if (ids.isEmpty()) {
                return List.of();
            }
            Map<String, ConsumerGroupDescription> descripciones =
                    esperar(admin.describeConsumerGroups(ids).all());
            List<ConsumerGroupInfo> resultado = new ArrayList<>();
            for (String id : ids) {
                resultado.add(armarGrupo(admin, descripciones.get(id)));
            }
            return resultado;
        }
    }

    public ConsumerGroupInfo consultarGrupo(String groupId) {
        try (AdminClient admin = factory.crear()) {
            ConsumerGroupDescription descripcion =
                    esperar(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
            ConsumerGroupInfo info = armarGrupo(admin, descripcion);
            if (info.members() == 0 && info.offsets().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El grupo no existe o no tiene datos: " + groupId);
            }
            return info;
        }
    }

    // ---------- Helpers ----------

    private List<TopicInfo> armarTopicos(AdminClient admin, Collection<String> nombres) {
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        List<ConfigResource> recursos = nombres.stream()
                .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n))
                .toList();
        Map<ConfigResource, Config> configs = esperar(admin.describeConfigs(recursos).all());

        List<TopicInfo> resultado = new ArrayList<>();
        for (String nombre : nombres) {
            TopicDescription d = descripciones.get(nombre);
            Config c = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, nombre));
            int replicas = d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
            resultado.add(new TopicInfo(nombre, d.partitions().size(), replicas,
                    valor(c, "cleanup.policy"),
                    aLong(valor(c, "retention.ms")),
                    aInteger(valor(c, "min.insync.replicas")),
                    TOPICOS_SISTEMA.contains(nombre)));
        }
        return resultado;
    }

    private ConsumerGroupInfo armarGrupo(AdminClient admin, ConsumerGroupDescription d) {
        String groupId = d.groupId();
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        for (TopicPartition tp : confirmados.keySet()) {
            pedido.put(tp, OffsetSpec.latest());
        }
        Map<TopicPartition, ListOffsetsResultInfo> finales =
                pedido.isEmpty() ? Map.of() : esperar(admin.listOffsets(pedido).all());

        List<GroupOffsetInfo> offsets = new ArrayList<>();
        long lagTotal = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : confirmados.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            long actual = e.getValue().offset();
            long fin = finales.get(e.getKey()).offset();
            long lag = Math.max(0, fin - actual);
            lagTotal += lag;
            offsets.add(new GroupOffsetInfo(e.getKey().topic(), e.getKey().partition(), actual, fin, lag));
        }
        offsets.sort(Comparator.comparing(GroupOffsetInfo::topic).thenComparingInt(GroupOffsetInfo::partition));

        return new ConsumerGroupInfo(groupId, String.valueOf(d.groupState()),
                d.members().size(), lagTotal, offsets);
    }

    private AlterConfigOp operacion(String clave, String valor) {
        return new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET);
    }

    private String valor(Config config, String clave) {
        ConfigEntry entrada = (config == null) ? null : config.get(clave);
        return (entrada == null) ? null : entrada.value();
    }

    private Long aLong(String texto) {
        return (texto == null) ? null : Long.valueOf(texto);
    }

    private Integer aInteger(String texto) {
        return (texto == null) ? null : Integer.valueOf(texto);
    }

    // Espera la respuesta de Kafka (máximo 8 segundos) y traduce los errores a respuestas HTTP
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operación interrumpida");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no respondió a tiempo");
        }
    }

    private ResponseStatusException traducir(Throwable causa) {
        if (causa instanceof TopicExistsException) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "El tópico ya existe");
        }
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "El tópico no existe");
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidPartitionsException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        log.error("[ADMIN-KAFKA] Error hablando con Kafka: {}", causa.getMessage());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Kafka no está disponible: " + causa.getMessage());
    }

    private String validarNombre(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del tópico es obligatorio");
        }
        String limpio = name.trim();
        if (limpio.length() > LARGO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los " + LARGO_MAXIMO + " caracteres");
        }
        if (!NOMBRE_VALIDO.matcher(limpio).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre solo puede tener letras, números, punto, guion y guion bajo (sin espacios)");
        }
        return limpio;
    }

    private void validarPolitica(String politica) {
        if (!POLITICAS.contains(politica)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cleanup.policy debe ser 'delete' o 'compact'");
        }
    }

    private void validarRetencion(Long ms) {
        if (ms == 0 || ms < -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "retentionMs debe ser positivo, o -1 para guardar para siempre");
        }
    }
}