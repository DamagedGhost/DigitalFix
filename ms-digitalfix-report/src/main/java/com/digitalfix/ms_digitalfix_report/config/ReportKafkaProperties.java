package com.digitalfix.ms_digitalfix_report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digitalfix.kafka")
public record ReportKafkaProperties(
        String bootstrapServers,
        String workordersTopic,
        String groupId) {
}