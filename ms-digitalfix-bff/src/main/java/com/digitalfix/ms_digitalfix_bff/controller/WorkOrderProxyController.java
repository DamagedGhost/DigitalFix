package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proxy del BFF hacia ms-digitalfix-workorders.
 * Soporta tanto /api/workorders como /api/v1/workorders para compatibilidad con Nginx local y API Gateway.
 * Cada método restringe qué rol puede ejecutar la acción antes de tocar el microservicio interno.
 */
@RestController
@RequestMapping({"/api/workorders", "/api/v1/workorders"})
public class WorkOrderProxyController {

    private final RestClient workOrdersRestClient;

    public WorkOrderProxyController(@Qualifier("workOrdersRestClient") RestClient workOrdersRestClient) {
        this.workOrdersRestClient = workOrdersRestClient;
    }

    // Crear nueva orden: permitido para Cliente, Supervisor y Admin
    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Cliente', 'Supervi', 'Supervisor')")
    public ResponseEntity<String> create(@RequestBody String body) {
        return forward(() -> workOrdersRestClient.post()
                .uri("/api/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Consultar orden puntual por ID (lectura abierta a todos los roles autenticados del sistema)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervi', 'Supervisor', 'Cliente', 'Auditor')")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return forward(() -> workOrdersRestClient.get()
                .uri("/api/workorders/{id}", id)
                .retrieve()
                .toEntity(String.class));
    }

    // Listar órdenes, pasando el filtro opcional ?status= si viene desde el front
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervi', 'Supervisor', 'Cliente', 'Auditor')")
    public ResponseEntity<String> list(@RequestParam(required = false) String status) {
        return forward(() -> workOrdersRestClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/workorders");
                    if (status != null) {
                        builder.queryParam("status", status);
                    }
                    return builder.build();
                })
                .retrieve()
                .toEntity(String.class));
    }

    // Cambiar estado de una orden (es lo que dispara los eventos a RabbitMQ y Kafka): solo Supervisor o Admin
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervi', 'Supervisor')")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody String body) {
        return forward(() -> workOrdersRestClient.put()
                .uri("/api/workorders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Endpoint auxiliar de diagnóstico para revisar qué usuario y roles leyó Spring desde el JWT
    @GetMapping("/debug/whoami")
    public ResponseEntity<String> whoami() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(
                "Principal: " + auth.getName() + "\n" +
                "Authorities: " + auth.getAuthorities()
        );
    }

    // Ejecuta la llamada al microservicio preservando el código HTTP si falló por negocio (400/404/409),
    // o devolviendo 503 JSON limpio si el contenedor de workorders está caído.
    private ResponseEntity<String> forward(java.util.function.Supplier<ResponseEntity<String>> call) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"El servicio de órdenes de trabajo no responde\"}");
        }
    }
}