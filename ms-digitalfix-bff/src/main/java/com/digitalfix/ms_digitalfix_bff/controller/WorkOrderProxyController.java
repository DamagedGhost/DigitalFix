package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderProxyController {

    private final RestClient workOrdersRestClient;

    public WorkOrderProxyController(RestClient workOrdersRestClient) {
        this.workOrdersRestClient = workOrdersRestClient;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Cliente', 'Supervi')")
    public ResponseEntity<String> create(@RequestBody String body) {
        return forward(() -> workOrdersRestClient.post()
                .uri("/api/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Supervi', 'Cliente', 'Auditor')")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return forward(() -> workOrdersRestClient.get()
                .uri("/api/workorders/{id}", id)
                .retrieve()
                .toEntity(String.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Admin', 'Supervi', 'Cliente', 'Auditor')")
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

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Supervi')")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody String body) {
        return forward(() -> workOrdersRestClient.put()
                .uri("/api/workorders/{id}/status", id)
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