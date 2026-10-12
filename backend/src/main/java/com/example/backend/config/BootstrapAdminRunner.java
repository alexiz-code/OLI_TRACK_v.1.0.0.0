package com.example.backend.config;

import com.example.backend.dto.UsuarioDTO;
import com.example.backend.model.Rol;
import com.example.backend.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-admin")
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UsuarioService usuarioService;

    public BootstrapAdminRunner(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {

        String nombre = obtenerVariable(
                "OLITRACK_BOOTSTRAP_ADMIN_NAME"
        );

        String email = obtenerVariable(
                "OLITRACK_BOOTSTRAP_ADMIN_EMAIL"
        );

        String password = obtenerVariable(
                "OLITRACK_BOOTSTRAP_ADMIN_PASSWORD"
        );

        String telefono = obtenerVariable(
                "OLITRACK_BOOTSTRAP_ADMIN_PHONE"
        );

        log.info("==============================================");
        log.info("   OLITRACK - BOOTSTRAP ADMINISTRADOR");
        log.info("==============================================");
        log.info("Creando administrador: {}", email);

        UsuarioDTO.CrearRequest request =
                new UsuarioDTO.CrearRequest();

        request.setNombre(nombre);
        request.setEmail(email);
        request.setPassword(password);
        request.setTelefono(telefono);
        request.setRol(Rol.ADMINISTRADOR);

        usuarioService.registrar(request, Rol.ADMINISTRADOR)
                .doOnSuccess(usuario -> {

                    log.info("==============================================");
                    log.info("ADMINISTRADOR CREADO CORRECTAMENTE");
                    log.info("==============================================");
                    log.info("UID: {}", usuario.getId());
                    log.info("Nombre: {}", usuario.getNombre());
                    log.info("Email: {}", usuario.getEmail());
                    log.info("Rol: {}", usuario.getRol());
                    log.info("Activo: {}", usuario.isActivo());
                    log.info("==============================================");

                })
                .doOnError(error -> {

                    log.error(
                            "No se pudo crear el administrador: {}",
                            error.getMessage()
                    );

                })
                .block();
    }

    private String obtenerVariable(String nombre) {

        String valor = System.getenv(nombre);

        if (valor == null || valor.isBlank()) {

            throw new IllegalStateException(
                    "Falta la variable de entorno: " + nombre
            );
        }

        return valor.trim();
    }
}