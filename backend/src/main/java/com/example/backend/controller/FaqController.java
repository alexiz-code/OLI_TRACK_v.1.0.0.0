package com.example.backend.controller;

import com.example.backend.dto.FaqDTO;
import com.example.backend.service.FaqService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Preguntas frecuentes (HU-082). Lectura pública; escritura solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<FaqDTO.Response>> crear(@Valid @RequestBody FaqDTO.Request request) {
        return faqService.crear(request).map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    public Flux<FaqDTO.Response> listarActivas() {
        return faqService.listarActivas();
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<FaqDTO.Response> listarTodas() {
        return faqService.listarTodas();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<FaqDTO.Response> actualizar(@PathVariable String id, @Valid @RequestBody FaqDTO.Request request) {
        return faqService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> desactivar(@PathVariable String id) {
        return faqService.cambiarActiva(id, false).thenReturn(ResponseEntity.noContent().build());
    }
}
