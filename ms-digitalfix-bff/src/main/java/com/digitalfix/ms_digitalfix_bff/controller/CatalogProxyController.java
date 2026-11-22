package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proxy del BFF hacia ms-digitalfix-catalog.
 * Admin puede crear y editar tarifas/stock; Supervisor solo tiene acceso de lectura.
 */
@RestController
@RequestMapping({"/api/catalog", "/api/v1/catalog"})
public class CatalogProxyController {

    private final RestClient catalogRestClient;

    public CatalogProxyController(@Qualifier("catalogRestClient") RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    // Alta de un nuevo servicio/repuesto en el catálogo (solo Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Administrador')")
    public ResponseEntity<String> create(@RequestBody String body) {
        return forward(() -> catalogRestClient.post()
                .uri("/api/catalog/services")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Listado completo del catálogo
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervi', 'Supervisor')")
    public ResponseEntity<String> list() {
        return forward(() -> catalogRestClient.get()
                .uri("/api/catalog/services")
                .retrieve()
                .toEntity(String.class));
    }

    // Consulta de un ítem específico por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervi', 'Supervisor')")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return forward(() -> catalogRestClient.get()
                .uri("/api/catalog/services/{id}", id)
                .retrieve()
                .toEntity(String.class));
    }

    // Actualización de tarifa y/o stock disponible (solo Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Administrador')")
    public ResponseEntity<String> updateTarifaStock(@PathVariable Long id, @RequestBody String body) {
        return forward(() -> catalogRestClient.put()
                .uri("/api/catalog/services/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    // Reenvía el error HTTP original del microservicio de catálogo si responde 4xx/5xx
    private ResponseEntity<String> forward(java.util.function.Supplier<ResponseEntity<String>> call) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        }
    }
}