package com.digitalfix.ms_digitalfix_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cuántas veces se reintenta un error temporal y cuánto se espera entre intentos.
 * Se lee del application.yaml.
 */
@ConfigurationProperties(prefix = "digitalfix.consumer.retry")
public record ConsumerRetryProperties(int maxAttempts, long backoffMs) {
}