package com.digitalfix.ms_digitalfix_notify.admin.kafka;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API del administrador de Kafka: tópicos y consumer groups.
 */
@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    // Lista de tópicos con su configuración
    @GetMapping("/topics")
    public List<TopicInfo> listarTopicos() {
        return service.listarTopicos();
    }

    // Un tópico por nombre
    @GetMapping("/topics/{name}")
    public TopicInfo consultarTopico(@PathVariable String name) {
        return service.consultarTopico(name);
    }

    // Crear un tópico nuevo
    @PostMapping("/topics")
    public ResponseEntity<TopicInfo> crear(@RequestBody CreateTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    // Modificar retención, política, min ISR o aumentar particiones
    @PatchMapping("/topics/{name}")
    public TopicInfo modificar(@PathVariable String name, @RequestBody UpdateTopicRequest request) {
        return service.modificar(name, request);
    }

    // Borrar un tópico (no deja borrar los del sistema)
    @DeleteMapping("/topics/{name}")
    public Map<String, Object> eliminar(@PathVariable String name) {
        service.eliminar(name);
        return Map.of("topic", name, "deleted", true);
    }

    // Consumer groups con su lag
    @GetMapping("/groups")
    public List<ConsumerGroupInfo> listarGrupos() {
        return service.listarGrupos();
    }

    // Un consumer group con el detalle por partición
    @GetMapping("/groups/{groupId}")
    public ConsumerGroupInfo consultarGrupo(@PathVariable String groupId) {
        return service.consultarGrupo(groupId);
    }
}