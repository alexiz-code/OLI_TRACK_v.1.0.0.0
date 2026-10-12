package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    private AuthDTO() {
    }

    /** Cuerpo esperado para iniciar sesión. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }

    /**
     * El login real (verificar contraseña) lo hace Firebase Authentication desde
     * el cliente (Angular con el SDK de Firebase); el backend solo valida el
     * idToken resultante. Este DTO se usa para el intercambio de un idToken
     * de Firebase por los datos de sesión del usuario en OLITRACK.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SesionResponse {
        private UsuarioDTO.Response usuario;
        private String mensaje;
    }
}
