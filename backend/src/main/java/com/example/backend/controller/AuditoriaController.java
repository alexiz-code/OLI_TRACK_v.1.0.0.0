package com.example.backend.controller;

import com.example.backend.dto.AuditDTO;
import com.example.backend.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Registro de actividad (HU-022, HU-023). Solo lectura. */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditService auditService;

    public AuditoriaController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Flux<AuditDTO.Response> listar(@RequestParam(required = false) String entidad) {
        return auditService.listar(entidad);
    }
}
