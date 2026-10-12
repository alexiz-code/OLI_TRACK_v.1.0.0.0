package com.example.backend.security;

import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SecurityErrorHandlers implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException ex) {

        return escribir(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "No autenticado. Envíe un idToken de Firebase válido."
        );
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException ex) {

        return escribir(
                exchange,
                HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta acción."
        );
    }

    private Mono<Void> escribir(
            ServerWebExchange exchange,
            HttpStatus status,
            String mensaje) {

        exchange.getResponse().setStatusCode(status);

        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        body.put("path", exchange.getRequest().getPath().value());

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = (
                    "{\"mensaje\":\"" +
                            mensaje.replace("\"", "\\\"") +
                            "\"}"
            ).getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer =
                exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes);

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }
}