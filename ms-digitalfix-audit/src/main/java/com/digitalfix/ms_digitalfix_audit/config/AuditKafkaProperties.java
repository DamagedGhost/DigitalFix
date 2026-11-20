package com.digitalfix.ms_digitalfix_audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digitalfix.kafka")
public record AuditKafkaProperties(
        String bootstrapServers,
        String auditTopic,
        String groupId) {
}