package com.digitalfix.ms_digitalfix_bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

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

    // Cliente hacia ms-digitalfix-notify (administrador de colas)
    @Bean
    public RestClient notifyRestClient(@Value("${notify.base-url}") String baseUrl) {
        return createClientBuilder(baseUrl).build();
    }
}