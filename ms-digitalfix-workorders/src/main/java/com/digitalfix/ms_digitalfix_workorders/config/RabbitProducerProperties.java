package com.digitalfix.ms_digitalfix_workorders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee del application.yaml el exchange y las llaves de ruteo.
 * Así el producer no tiene nombres escritos a mano.
 */
@ConfigurationProperties(prefix = "digitalfix.rabbit")
public record RabbitProducerProperties(String exchange, RoutingKeys routingKeys) {

    public record RoutingKeys(String email, String dispatch, String report) {}
}