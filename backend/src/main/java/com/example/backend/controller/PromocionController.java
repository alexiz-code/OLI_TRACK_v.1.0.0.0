package com.example.backend.controller;

import com.example.backend.dto.PromocionDTO;
import com.example.backend.service.PromocionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Promociones (HU-081). Lectura pública; escritura solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<PromocionDTO.Response>> crear(@Valid @RequestBody PromocionDTO.Request request) {
        return promocionService.crear(request).map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    public Flux<PromocionDTO.Response> listarVigentes() {
        return promocionService.listarVigentes();
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<PromocionDTO.Response> listarTodas() {
        return promocionService.listarTodas();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<PromocionDTO.Response> actualizar(@PathVariable String id, @Valid @RequestBody PromocionDTO.Request request) {
        return promocionService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> desactivar(@PathVariable String id) {
        return promocionService.cambiarActiva(id, false).thenReturn(ResponseEntity.noContent().build());
    }
}
