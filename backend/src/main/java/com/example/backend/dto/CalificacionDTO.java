package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class CalificacionDTO {

    private CalificacionDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrearRequest {

        @NotNull(message = "La cantidad de estrellas es obligatoria")
        @Min(value = 1, message = "Mínimo 1 estrella")
        @Max(value = 5, message = "Máximo 5 estrellas")
        private Integer estrellas;

        private String comentario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private int estrellas;
        private String comentario;
        private Date fecha;
    }
}
