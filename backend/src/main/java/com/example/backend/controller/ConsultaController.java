package com.example.backend.controller;

import com.example.backend.dto.ConsultaDTO;
import com.example.backend.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** POST es público (visitante sin cuenta); el resto es solo para el administrador. */
@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    public Mono<ResponseEntity<ConsultaDTO.Response>> registrar(@Valid @RequestBody ConsultaDTO.CrearRequest request) {
        return consultaService.registrar(request)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<ConsultaDTO.Response> listar() {
        return consultaService.listar();
    }

    @PatchMapping("/{id}/atendida")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ConsultaDTO.Response> marcarAtendida(@PathVariable String id) {
        return consultaService.marcarAtendida(id);
    }
}
