package com.digitalfix.ms_digitalfix_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee del application.yaml todos los nombres de exchanges y colas.
 * Así ninguna otra clase tiene que escribir nombres a mano.
 */
@ConfigurationProperties(prefix = "digitalfix.rabbit")
public record DigitalfixRabbitProperties(Exchanges exchanges, Queues queues) {

    public record Exchanges(String direct, String topic, String dead) {}

    public record Queues(QueueDef email, QueueDef dispatch, QueueDef report) {}

    /**
     * Una cola con su DLQ y las llaves de ruteo.
     * directKey y topicKey son las que usa el producer; dlqKey es la que usa el DLX.
     */
    public record QueueDef(String name, String dlq, String directKey, String topicKey, String dlqKey) {}
}