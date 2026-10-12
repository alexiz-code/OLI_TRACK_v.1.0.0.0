package com.example.backend.dto;

import com.example.backend.model.EstadoConsulta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class ConsultaDTO {

    private ConsultaDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrearRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
        private String nombre;

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        private String email;

        @NotBlank(message = "El asunto es obligatorio")
        private String asunto;

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 2000, message = "El mensaje no debe superar 2000 caracteres")
        private String mensaje;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String nombre;
        private String email;
        private String asunto;
        private String mensaje;
        private EstadoConsulta estado;
        private Date creadoEn;
    }
}
