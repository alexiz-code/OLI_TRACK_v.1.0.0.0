package com.example.backend.controller;

import com.example.backend.dto.UsuarioDTO;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Solo un administrador puede crear cuentas con rol arbitrario (incluido REPARTIDOR/ADMINISTRADOR). */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<UsuarioDTO.Response>> crear(@Valid @RequestBody UsuarioDTO.CrearRequest request) {
        return usuarioService.registrar(request, null)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Flux<UsuarioDTO.Response> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/me")
    public Mono<UsuarioDTO.Response> obtenerPerfilPropio() {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> usuarioService.obtenerPorId(actual.getUid()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<UsuarioDTO.Response> obtenerPorId(@PathVariable String id) {
        return usuarioService.obtenerPorId(id);
    }

    @PutMapping("/me")
    public Mono<UsuarioDTO.Response> actualizarPerfilPropio(@Valid @RequestBody UsuarioDTO.ActualizarRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> usuarioService.actualizar(actual.getUid(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<UsuarioDTO.Response> actualizarUsuario(@PathVariable String id,
                                                        @Valid @RequestBody UsuarioDTO.ActualizarRequest request) {
        return usuarioService.actualizar(id, request);
    }

    /** Baja lógica (no borra el documento ni el historial asociado). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> desactivar(@PathVariable String id) {
        return usuarioService.desactivar(id).thenReturn(ResponseEntity.noContent().build());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<ResponseEntity<Void>> reactivar(@PathVariable String id) {
        return usuarioService.reactivar(id).thenReturn(ResponseEntity.noContent().build());
    }

    /** HU-008: genera un enlace de restablecimiento de contraseña de Firebase. */
    @org.springframework.web.bind.annotation.PostMapping("/{id}/restablecer-password")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public reactor.core.publisher.Mono<com.example.backend.dto.UsuarioDTO.ResetLinkResponse> restablecerPassword(
            @org.springframework.web.bind.annotation.PathVariable String id) {
        return usuarioService.generarEnlaceRestablecimiento(id);
    }

    /** Gestión de permisos: cambia el rol de un usuario ya existente. */
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mono<UsuarioDTO.Response> cambiarRol(@PathVariable String id,
                                                  @Valid @RequestBody UsuarioDTO.CambiarRolRequest request) {
        return usuarioService.cambiarRol(id, request.getRol());
    }
}
