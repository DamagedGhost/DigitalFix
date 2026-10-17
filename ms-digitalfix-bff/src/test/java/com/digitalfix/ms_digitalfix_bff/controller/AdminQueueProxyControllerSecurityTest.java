package com.digitalfix.ms_digitalfix_bff.controller;

import com.digitalfix.ms_digitalfix_bff.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que el administrador de colas exige el rol Admin
 * y que el token con roles ["Admin"] se convierte en ROLE_Admin.
 */
class AdminQueueProxyControllerSecurityTest {

    private final JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter();

    private Jwt jwtConRoles(List<String> roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .subject("usuario-prueba");
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }

    @Test
    void controller_deberiaExigirRolAdmin() {
        PreAuthorize anotacion = AdminQueueProxyController.class.getAnnotation(PreAuthorize.class);

        assertThat(anotacion).isNotNull();
        assertThat(anotacion.value()).isEqualTo("hasRole('Admin')");
    }

    @Test
    void tokenConRolAdmin_deberiaConvertirseEnRoleAdmin() {
        var auth = converter.convert(jwtConRoles(List.of("Admin")));

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_Admin");
    }

    @Test
    void tokenConOtroRol_noDeberiaTenerRoleAdmin() {
        var auth = converter.convert(jwtConRoles(List.of("Cliente")));

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_Cliente")
                .doesNotContain("ROLE_Admin");
    }

    @Test
    void tokenSinRoles_noDeberiaTenerNingunRol() {
        var auth = converter.convert(jwtConRoles(null));

        assertThat(auth).isNotNull();
        // Spring Security 7 agrega FACTOR_BEARER a todo token; lo que importa es que no haya roles
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }
}