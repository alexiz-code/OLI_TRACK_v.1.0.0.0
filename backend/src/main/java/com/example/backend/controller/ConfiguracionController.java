package com.example.backend.controller;

import com.example.backend.dto.ConfiguracionDTO;
import com.example.backend.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Configuración institucional (HU-009, HU-019, HU-020). Lectura pública (el visitante ve horarios/contacto). */
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public Mono<ConfiguracionDTO.Datos> obtener() {
        return configuracionService.obtenerDatos();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ConfiguracionDTO.Datos> actualizar(@Valid @RequestBody ConfiguracionDTO.Datos datos) {
        return configuracionService.actualizar(datos);
    }
}
