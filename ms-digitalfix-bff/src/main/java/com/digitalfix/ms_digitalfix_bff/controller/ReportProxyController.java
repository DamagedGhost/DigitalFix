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
 * Proxy hacia ms-digitalfix-report (analítica en streaming sobre workorders.events).
 * Solo Admin; vale para todo el controller.
 */
@RestController
@RequestMapping({"/api/reports", "/api/v1/reports"})
@PreAuthorize("hasAnyRole('Admin', 'Administrador', 'Supervisor') or hasAnyAuthority('Admin', 'ROLE_Admin', 'Administrador', 'ROLE_Administrador', 'Supervisor', 'ROLE_Supervisor')")
public class ReportProxyController {

    private final RestClient reportRestClient;

    public ReportProxyController(@Qualifier("reportRestClient") RestClient reportRestClient) {
        this.reportRestClient = reportRestClient;
    }

    // Reporte de órdenes (contadores por estado y técnico)
    @GetMapping("/orders")
    public ResponseEntity<String> orders(@RequestParam MultiValueMap<String, String> params) {
        return forward(() -> reportRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/reports/orders")
                        .queryParams(params)
                        .build())
                .retrieve()
                .toEntity(String.class));
    }

    // Si report no responde, devuelve 503 con cuerpo JSON en vez de reventar
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
                    .body("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"El servicio de reportes no responde\"}");
        }
    }
}