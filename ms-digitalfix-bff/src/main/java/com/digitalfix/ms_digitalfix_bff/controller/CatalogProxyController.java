package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/catalog/services")
public class CatalogProxyController {

    private final RestClient catalogRestClient;

    public CatalogProxyController(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Admin')")
    public ResponseEntity<String> create(@RequestBody String body) {
        return forward(() -> catalogRestClient.post()
                .uri("/api/catalog/services")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Admin')")
    public ResponseEntity<String> list() {
        return forward(() -> catalogRestClient.get()
                .uri("/api/catalog/services")
                .retrieve()
                .toEntity(String.class));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin')")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return forward(() -> catalogRestClient.get()
                .uri("/api/catalog/services/{id}", id)
                .retrieve()
                .toEntity(String.class));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin')")
    public ResponseEntity<String> updateTarifaStock(@PathVariable Long id, @RequestBody String body) {
        return forward(() -> catalogRestClient.put()
                .uri("/api/catalog/services/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

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