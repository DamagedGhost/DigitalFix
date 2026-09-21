package com.digitalfix.ms_digitalfix_bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient workOrdersRestClient(@Value("${workorders.base-url}") String baseUrl) {
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
                })
                .build();
    }

    @Bean
    public RestClient catalogRestClient(@Value("${catalog.base-url}") String baseUrl) {
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
                })
                .build();
    }
}