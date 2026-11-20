package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Proxy hacia el administrador de Kafka de ms-digitalfix-notify (tópicos y consumer groups).
 * Solo Admin; vale para todo el controller.
 */
@RestController
@RequestMapping({"/api/admin/kafka", "/api/v1/admin/kafka"})
@PreAuthorize("hasAnyRole('Admin', 'Administrador') or hasAnyAuthority('Admin', 'ROLE_Admin', 'Administrador', 'ROLE_Administrador')")
public class KafkaAdminProxyController {

    private final RestClient notifyRestClient;

    public KafkaAdminProxyController(@Qualifier("notifyRestClient") RestClient notifyRestClient) {
        this.notifyRestClient = notifyRestClient;
    }

    // Listar tópicos
    @GetMapping("/topics")
    public ResponseEntity<String> listTopics() {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/kafka/topics")
                .retrieve()
                .toEntity(String.class));
    }

    // Describir un tópico por nombre
    @GetMapping("/topics/{name}")
    public ResponseEntity<String> getTopic(@PathVariable String name) {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/kafka/topics/{name}", name)
                .retrieve()
                .toEntity(String.class));
    }

    // Crear un tópico
    @PostMapping("/topics")
    public ResponseEntity<String> createTopic(@RequestBody String body) {
        return forward(() -> notifyRestClient.post()
                .uri("/api/admin/kafka/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Modificar la configuración de un tópico
    @PatchMapping("/topics/{name}")
    public ResponseEntity<String> updateTopic(@PathVariable String name, @RequestBody String body) {
        return forward(() -> notifyRestClient.patch()
                .uri("/api/admin/kafka/topics/{name}", name)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Eliminar un tópico
    @DeleteMapping("/topics/{name}")
    public ResponseEntity<String> deleteTopic(@PathVariable String name) {
        return forward(() -> notifyRestClient.delete()
                .uri("/api/admin/kafka/topics/{name}", name)
                .retrieve()
                .toEntity(String.class));
    }

    // Listar consumer groups
    @GetMapping("/groups")
    public ResponseEntity<String> listGroups() {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/kafka/groups")
                .retrieve()
                .toEntity(String.class));
    }

    // Describir un consumer group (miembros, offsets, lag)
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<String> getGroup(@PathVariable String groupId) {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/kafka/groups/{groupId}", groupId)
                .retrieve()
                .toEntity(String.class));
    }

    // Si notify no responde, devuelve 503 con cuerpo JSON en vez de reventar
    private ResponseEntity<String> forward(Supplier<ResponseEntity<String>> call) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"El servicio de notificaciones no responde\"}");
        }
    }
}