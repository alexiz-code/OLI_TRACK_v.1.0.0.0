package com.example.backend.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Lee el header "Authorization: Bearer <idToken>" de la petición.
 * Si no existe, no hay autenticación para esta request (rutas públicas seguirán
 * su curso; las protegidas serán rechazadas más adelante en la cadena).
 */
public class FirebaseAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String PREFIJO_BEARER = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header) || !header.startsWith(PREFIJO_BEARER)) {
            return Mono.empty();
        }

        String idToken = header.substring(PREFIJO_BEARER.length()).trim();
        if (!StringUtils.hasText(idToken)) {
            return Mono.empty();
        }

        return Mono.just(new FirebaseAuthenticationToken(idToken));
    }
}
