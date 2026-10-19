package com.example.backend.controller;

import com.example.backend.dto.ZonaDTO;
import com.example.backend.service.ZonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Zonas de reparto (HU-016 a HU-018). Lectura pública para que el visitante vea cobertura; escritura solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<ZonaDTO.Response>> crear(@Valid @RequestBody ZonaDTO.Request request) {
        return zonaService.crear(request).map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    public Flux<ZonaDTO.Response> listar() {
        return zonaService.listar();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<ZonaDTO.Response> listarTodas() {
        return zonaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Mono<ZonaDTO.Response> obtenerPorId(@PathVariable String id) {
        return zonaService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ZonaDTO.Response> actualizar(@PathVariable String id, @Valid @RequestBody ZonaDTO.Request request) {
        return zonaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> desactivar(@PathVariable String id) {
        return zonaService.cambiarActiva(id, false).thenReturn(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> reactivar(@PathVariable String id) {
        return zonaService.cambiarActiva(id, true).thenReturn(ResponseEntity.noContent().build());
    }
}
