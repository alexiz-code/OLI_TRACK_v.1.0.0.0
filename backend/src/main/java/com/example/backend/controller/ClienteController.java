package com.example.backend.controller;

import com.example.backend.dto.ClienteDTO;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /** Completa el perfil de negocio justo después del registro (POST /api/auth/registro). */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ResponseEntity<ClienteDTO.Response>> crear(@Valid @RequestBody ClienteDTO.CrearRequest request) {
        return clienteService.crear(request)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Flux<ClienteDTO.Response> listar() {
        return clienteService.listar();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ClienteDTO.Response> obtenerPerfilPropio() {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR', 'REPARTIDOR')")
    public Mono<ClienteDTO.Response> obtenerPorId(@PathVariable String id) {
        return clienteService.obtenerPorId(id);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ClienteDTO.Response> actualizarPerfilPropio(@Valid @RequestBody ClienteDTO.ActualizarRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()))
                .flatMap(perfil -> clienteService.actualizar(perfil.getId(), request));
    }

    /** HU-057: el cliente agrega otra dirección de entrega. */
    @PostMapping("/me/direcciones")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ClienteDTO.Response> agregarDireccion(@Valid @RequestBody ClienteDTO.DireccionCrearRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()))
                .flatMap(cliente -> clienteService.agregarDireccion(cliente.getId(), request));
    }

    @DeleteMapping("/me/direcciones/{direccionId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ClienteDTO.Response> eliminarDireccion(@PathVariable String direccionId) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()))
                .flatMap(cliente -> clienteService.eliminarDireccion(cliente.getId(), direccionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> desactivar(@PathVariable String id) {
        return clienteService.desactivar(id).thenReturn(ResponseEntity.noContent().build());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> reactivar(@PathVariable String id) {
        return clienteService.reactivar(id).thenReturn(ResponseEntity.noContent().build());
    }
}
