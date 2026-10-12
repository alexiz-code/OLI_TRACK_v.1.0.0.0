package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FaqDTO {

    private FaqDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "La pregunta es obligatoria")
        private String pregunta;

        @NotBlank(message = "La respuesta es obligatoria")
        private String respuesta;

        private int orden;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String pregunta;
        private String respuesta;
        private int orden;
        private boolean activa;
    }
}
