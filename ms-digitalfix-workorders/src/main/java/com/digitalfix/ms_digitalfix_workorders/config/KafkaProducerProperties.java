package com.digitalfix.ms_digitalfix_workorders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lo que se lee de digitalfix.kafka en el application.yaml.
 */
@ConfigurationProperties(prefix = "digitalfix.kafka")
public record KafkaProducerProperties(
        String bootstrapServers,
        String topic) {
}