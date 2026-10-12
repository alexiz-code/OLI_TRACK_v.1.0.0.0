package com.example.backend.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static Mono<UsuarioAutenticado> usuarioActual() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UsuarioAutenticado) ctx.getAuthentication().getPrincipal());
    }
}
