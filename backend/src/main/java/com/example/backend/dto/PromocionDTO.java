package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class PromocionDTO {

    private PromocionDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "El título es obligatorio")
        private String titulo;

        private String descripcion;
        private Date vigenteDesde;
        private Date vigenteHasta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String titulo;
        private String descripcion;
        private Date vigenteDesde;
        private Date vigenteHasta;
        private boolean activa;
        private Date creadoEn;
    }
}
