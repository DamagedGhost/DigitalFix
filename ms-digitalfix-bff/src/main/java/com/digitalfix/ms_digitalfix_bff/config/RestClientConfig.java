package com.digitalfix.ms_digitalfix_bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Clientes HTTP internos del BFF hacia los 5 microservicios de dominio.
 * Las URLs vienen desde application.yaml / variables de entorno de Docker Compose
 * para no dejar IPs ni puertos fijos en el código.
 */
@Configuration
public class RestClientConfig {

    // Interceptor común: toma el header Authorization de la petición original y lo propaga
    // al microservicio interno por si necesita identificar al usuario autenticado.
    private RestClient.Builder createClientBuilder(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String authorization = attributes.getRequest().getHeader("Authorization");
                        if (authorization != null) {
                            request.getHeaders().set("Authorization", authorization);
                        }
                    }
                    return execution.execute(request, body);
                });
    }

    @Bean
    public RestClient workOrdersRestClient(@Value("${workorders.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }

    @Bean
    public RestClient catalogRestClient(@Value("${catalog.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }

    // Cliente hacia ms-digitalfix-notify (administrador de colas y de Kafka)
    @Bean
    public RestClient notifyRestClient(@Value("${notify.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }

    // Cliente hacia ms-digitalfix-audit (consumidor de audit.events)
    @Bean
    public RestClient auditRestClient(@Value("${audit.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }

    // Cliente hacia ms-digitalfix-report (analítica sobre workorders.events)
    @Bean
    public RestClient reportRestClient(@Value("${report.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }
}