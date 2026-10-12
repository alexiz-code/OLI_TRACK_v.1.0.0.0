package com.example.backend.controller;

import com.example.backend.dto.IncidenciaDTO;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.service.IncidenciaService;
import com.example.backend.service.RepartidorService;
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

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final RepartidorService repartidorService;

    public IncidenciaController(IncidenciaService incidenciaService, RepartidorService repartidorService) {
        this.incidenciaService = incidenciaService;
        this.repartidorService = repartidorService;
    }

    /** Repartidor: registra sobre su propia entrega. Admin/Supervisor: sobre cualquier pedido (HU-043). */
    @PostMapping
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<ResponseEntity<IncidenciaDTO.Response>> registrar(@Valid @RequestBody IncidenciaDTO.CrearRequest request) {
        return SecurityContextUtils.usuarioActual().flatMap(actual -> {
            Mono<IncidenciaDTO.Response> resultado = "REPARTIDOR".equals(actual.getRol().name())
                    ? repartidorService.obtenerPorUsuarioId(actual.getUid())
                            .flatMap(repartidor -> incidenciaService.registrar(repartidor.getId(), request))
                    : incidenciaService.registrarComoSupervisor(request);
            return resultado.map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
        });
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Flux<IncidenciaDTO.Response> listar() {
        return incidenciaService.listar();
    }

    @GetMapping("/pedido/{pedidoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR', 'REPARTIDOR')")
    public Flux<IncidenciaDTO.Response> listarPorPedido(@PathVariable String pedidoId) {
        return incidenciaService.listarPorPedido(pedidoId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR', 'REPARTIDOR')")
    public Mono<IncidenciaDTO.Response> obtenerPorId(@PathVariable String id) {
        return incidenciaService.obtenerPorId(id);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<IncidenciaDTO.Response> cambiarEstado(@PathVariable String id,
                                                        @Valid @RequestBody IncidenciaDTO.CambiarEstadoRequest request) {
        return incidenciaService.cambiarEstado(id, request.getEstado());
    }
}
