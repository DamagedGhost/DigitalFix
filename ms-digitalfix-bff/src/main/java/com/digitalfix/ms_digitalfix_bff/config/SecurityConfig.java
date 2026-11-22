package com.digitalfix.ms_digitalfix_bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del BFF como Resource Server de Microsoft Entra ID.
 * Acá valido el JWT entrante, extraigo el claim "roles" del token y aplico
 * las reglas de acceso por ruta antes de reenviar la petición a los microservicios.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API REST con JWT: no uso sesiones en servidor ni cookies, así que desactivo CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Rutas de Kafka: auditoría (Admin/Auditor), reportes (Admin/Supervisor) y administración (solo Admin)
                        .requestMatchers("/api/audit/**", "/api/v1/audit/**")
                        .hasAnyRole("Admin", "Administrador", "Auditor")
                        .requestMatchers("/api/reports/**", "/api/v1/reports/**")
                        .hasAnyRole("Admin", "Administrador", "Supervisor")
                        .requestMatchers("/api/admin/kafka/**", "/api/v1/admin/kafka/**")
                        .hasAnyRole("Admin", "Administrador")
                        // El resto exige estar autenticado y el detalle de roles se controla con @PreAuthorize en cada controller
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 403: Usuario autenticado pero sin rol suficiente (devuelvo JSON claro para que el front muestre el aviso)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Tu rol no tiene permiso para esta acción\"}"
                            );
                        })
                        // 401: Petición sin token, token expirado o firma inválida
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token de autenticación no proporcionado o inválido\"}"
                            );
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * Por defecto Spring busca scopes ("scp"), pero Entra ID manda los perfiles en el claim "roles".
     * Le agrego el prefijo "ROLE_" para que funcione directo con hasAnyRole(...) en SecurityConfig y @PreAuthorize.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}