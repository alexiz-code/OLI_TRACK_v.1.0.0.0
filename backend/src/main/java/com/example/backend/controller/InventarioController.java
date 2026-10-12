package com.example.backend.controller;

import com.example.backend.dto.InventarioDTO;
import com.example.backend.service.InventarioService;
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

/** Todo el módulo de inventario es de uso administrativo. */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<InventarioDTO.Response>> crear(@Valid @RequestBody InventarioDTO.CrearRequest request) {
        return inventarioService.crear(request)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<InventarioDTO.Response> listar() {
        return inventarioService.listar();
    }

    @GetMapping("/bajo-minimo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<InventarioDTO.Response> listarBajoMinimo() {
        return inventarioService.listarBajoMinimo();
    }

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<InventarioDTO.Response> obtenerPorProductoId(@PathVariable String productoId) {
        return inventarioService.obtenerPorProductoId(productoId);
    }

    /** Historial de movimientos de stock del producto (trazabilidad). */
    @GetMapping("/producto/{productoId}/kardex")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<com.example.backend.dto.KardexDTO.Response> obtenerKardex(@PathVariable String productoId) {
        return inventarioService.obtenerKardex(productoId);
    }

    /** Registra una entrada, salida o ajuste; es la única vía para cambiar el stock. */
    @PatchMapping("/{id}/movimiento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<InventarioDTO.Response> registrarMovimiento(@PathVariable String id,
                                                              @Valid @RequestBody InventarioDTO.MovimientoRequest request) {
        return inventarioService.registrarMovimiento(id, request);
    }
}
