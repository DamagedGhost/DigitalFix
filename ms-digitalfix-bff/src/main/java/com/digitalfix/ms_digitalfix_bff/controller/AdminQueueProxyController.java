package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Deja pasar las llamadas al administrador de colas de notify,
 * pero solo si el usuario tiene el rol Admin (vale para todo el controller).
 */
@RestController
@RequestMapping("/api/admin/queues")
@PreAuthorize("hasRole('Admin')")
public class AdminQueueProxyController {

    private final RestClient notifyRestClient;

    public AdminQueueProxyController(@Qualifier("notifyRestClient") RestClient notifyRestClient) {
        this.notifyRestClient = notifyRestClient;
    }

    // Estado de las colas y DLQ del sistema
    @GetMapping
    public ResponseEntity<String> list() {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/queues")
                .retrieve()
                .toEntity(String.class));
    }

    // Estado de una cola por nombre
    @GetMapping("/{name}")
    public ResponseEntity<String> get(@PathVariable String name) {
        return forward(() -> notifyRestClient.get()
                .uri("/api/admin/queues/{name}", name)
                .retrieve()
                .toEntity(String.class));
    }

    // Crear una cola
    @PostMapping
    public ResponseEntity<String> create(@RequestBody String body) {
        return forward(() -> notifyRestClient.post()
                .uri("/api/admin/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Borrar una cola
    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        return forward(() -> notifyRestClient.delete()
                .uri("/api/admin/queues/{name}", name)
                .retrieve()
                .toEntity(String.class));
    }

    // Vaciar la cola principal o la DLQ de un flujo
    @DeleteMapping("/{flow}/purge")
    public ResponseEntity<String> purge(@PathVariable String flow,
                                        @RequestParam(defaultValue = "false") boolean dlq) {
        return forward(() -> notifyRestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/queues/{flow}/purge")
                        .queryParam("dlq", dlq)
                        .build(flow))
                .retrieve()
                .toEntity(String.class));
    }

    // Reprocesar los mensajes de una DLQ
    @PostMapping("/{flow}/reprocess")
    public ResponseEntity<String> reprocess(@PathVariable String flow,
                                            @RequestParam(defaultValue = "100") int max) {
        return forward(() -> notifyRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/queues/{flow}/reprocess")
                        .queryParam("max", max)
                        .build(flow))
                .retrieve()
                .toEntity(String.class));
    }

    // Devuelve tal cual el estado y el cuerpo que respondió notify (400, 404, 409...)
    private ResponseEntity<String> forward(Supplier<ResponseEntity<String>> call) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            // notify está caído o no responde
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"El servicio de notificaciones no está disponible\"}");
        }
    }
}