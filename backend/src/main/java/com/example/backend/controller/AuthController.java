package com.example.backend.controller;

import com.example.backend.dto.UsuarioDTO;
import com.example.backend.model.Rol;
import com.example.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controlador de autenticación y registro.
 *
 * El registro público permite crear únicamente usuarios CLIENTE.
 *
 * El endpoint /bootstrap-admin es temporal y sirve para crear el primer
 * administrador del sistema cuando todavía no existe ninguno.
 *
 * Una vez creado el administrador, este endpoint debe eliminarse.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    @Value("${app.bootstrap-admin.secret:}")
    private String bootstrapAdminSecret;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Registro público.
     *
     * Aunque el cliente envíe otro rol, el servicio fuerza CLIENTE.
     */
    @PostMapping("/registro")
    public Mono<ResponseEntity<UsuarioDTO.Response>> registrarCliente(
            @Valid @RequestBody UsuarioDTO.CrearRequest request) {

        return usuarioService.registrar(request, Rol.CLIENTE)
                .map(dto -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(dto));
    }

    /**
     * Bootstrap temporal del primer administrador.
     *
     * Este endpoint requiere una clave secreta definida en
     * application.properties.
     *
     * IMPORTANTE:
     * Después de crear el primer administrador, eliminar este endpoint.
     */
    @PostMapping("/bootstrap-admin")
    public Mono<ResponseEntity<UsuarioDTO.Response>> bootstrapAdmin(
            @RequestHeader(value = "X-Bootstrap-Secret", required = false)
            String secret,
            @Valid @RequestBody UsuarioDTO.CrearRequest request) {

        if (bootstrapAdminSecret == null
                || bootstrapAdminSecret.isBlank()
                || secret == null
                || !bootstrapAdminSecret.equals(secret)) {

            return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            );
        }

        return usuarioService.registrar(request, Rol.ADMINISTRADOR)
                .map(dto -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(dto));
    }
}