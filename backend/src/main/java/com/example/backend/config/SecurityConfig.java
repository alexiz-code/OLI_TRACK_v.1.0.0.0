package com.example.backend.config;

import com.example.backend.security.FirebaseAuthenticationConverter;
import com.example.backend.security.FirebaseReactiveAuthenticationManager;
import com.example.backend.security.SecurityErrorHandlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;

import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final FirebaseReactiveAuthenticationManager authenticationManager;
    private final SecurityErrorHandlers errorHandlers;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            FirebaseReactiveAuthenticationManager authenticationManager,
            SecurityErrorHandlers errorHandlers,
            CorsConfigurationSource corsConfigurationSource) {

        this.authenticationManager = authenticationManager;
        this.errorHandlers = errorHandlers;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        AuthenticationWebFilter authenticationWebFilter =
                new AuthenticationWebFilter(authenticationManager);

        authenticationWebFilter.setServerAuthenticationConverter(
                new FirebaseAuthenticationConverter()
        );

        return http

                // CORS
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )

                // CSRF desactivado porque usamos API REST + Firebase Token
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // No usamos Basic Auth
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // No usamos formulario de login de Spring
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // No usamos logout de Spring
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                // Manejo de errores de autenticación/autorización
                .exceptionHandling(handling ->
                        handling
                                .authenticationEntryPoint(errorHandlers)
                                .accessDeniedHandler(errorHandlers)
                )

                // Nuestro filtro que valida Firebase ID Token
                .addFilterAt(
                        authenticationWebFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                // Autorizaciones
                .authorizeExchange(exchange -> exchange

                        // Actuator público
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Registro público de clientes
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/registro"
                        ).permitAll()

                        // Consultas públicas
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/consultas"
                        ).permitAll()

                        // Productos públicos para consultar
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/productos",
                                "/api/productos/**"
                        ).permitAll()

                        // Zonas públicas
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/zonas",
                                "/api/zonas/**"
                        ).permitAll()

                        // Configuración pública
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/configuracion"
                        ).permitAll()

                        // Promociones públicas
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/promociones"
                        ).permitAll()

                        // Preguntas frecuentes públicas
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/faqs"
                        ).permitAll()

                        // Repartidores disponibles
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/repartidores/disponibles"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "SUPERVISOR",
                                "CLIENTE"
                        )

                        // Todo lo demás necesita autenticación
                        .anyExchange().authenticated()
                )

                // IMPORTANTE: finaliza la configuración
                .build();
    }
}