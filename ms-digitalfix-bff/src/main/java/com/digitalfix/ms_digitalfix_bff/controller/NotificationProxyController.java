package com.digitalfix.ms_digitalfix_bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Proxy para el feed de eventos y notificaciones asíncronas de ms-digitalfix-notify.
 * Permite a cualquier usuario autenticado consultar los eventos recientes para el Dashboard.
 */
@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
public class NotificationProxyController {

    private final RestClient notifyRestClient;

    public NotificationProxyController(@Qualifier("notifyRestClient") RestClient notifyRestClient) {
        this.notifyRestClient = notifyRestClient;
    }

    @GetMapping
    public ResponseEntity<String> list() {
        return forward(() -> notifyRestClient.get()
                .uri("/api/notifications")
                .retrieve()
                .toEntity(String.class));
    }

    private ResponseEntity<String> forward(Supplier<ResponseEntity<String>> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":\"El servicio de notificaciones no responde\"}");
        }
    }
}
