package com.example.backend.controller;

import com.example.backend.dto.NotificacionDTO;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.service.NotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/mias")
    public Flux<NotificacionDTO.Response> misNotificaciones() {
        return SecurityContextUtils.usuarioActual()
                .flatMapMany(actual -> notificacionService.listarPropias(actual.getUid()));
    }

    @PatchMapping("/{id}/leida")
    public Mono<NotificacionDTO.Response> marcarLeida(@PathVariable String id) {
        return notificacionService.marcarLeida(id);
    }
}
