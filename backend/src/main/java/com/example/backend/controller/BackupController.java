package com.example.backend.controller;

import com.example.backend.service.BackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * HU-024: exporta una copia de seguridad lógica en JSON.
 * HU-025 (restaurar) se apoya en los respaldos automáticos que administra
 * Firestore/Google Cloud (point-in-time recovery), no en un endpoint propio:
 * restaurar datos de producción por API es un riesgo que este proyecto evita
 * a propósito. Ver README para el procedimiento de restauración real.
 */
@RestController
@RequestMapping("/api/backup")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/exportar")
    public Mono<ResponseEntity<Map<String, Object>>> exportar() {
        return backupService.exportar().map(data -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"olitrack-backup.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data));
    }
}
