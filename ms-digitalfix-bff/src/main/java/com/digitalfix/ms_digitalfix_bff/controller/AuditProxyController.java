package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Proxy hacia ms-digitalfix-audit (consumidor Kafka de audit.events).
 * Solo Admin; vale para todo el controller.
 */
@RestController
@RequestMapping({"/api/audit", "/api/v1/audit"})
@PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Auditor') or hasAnyAuthority('Admin', 'ROLE_Admin', 'Administrador', 'ROLE_Administrador', 'Auditor', 'ROLE_Auditor')")
public class AuditProxyController {

    private final RestClient auditRestClient;

    public AuditProxyController(@Qualifier("auditRestClient") RestClient auditRestClient) {
        this.auditRestClient = auditRestClient;
    }

    // Lista los eventos de auditoría; reenvía todos los query params que lleguen
    @GetMapping
    public ResponseEntity<String> list(@RequestParam MultiValueMap<String, String> params) {
        return forward(() -> auditRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit")
                        .queryParams(params)
                        .build())
                .retrieve()
                .toEntity(String.class));
    }

    // Si audit no responde, devuelve 503 con cuerpo JSON en vez de reventar
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
                    .body("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"El servicio de auditoría no responde\"}");
        }
    }
}