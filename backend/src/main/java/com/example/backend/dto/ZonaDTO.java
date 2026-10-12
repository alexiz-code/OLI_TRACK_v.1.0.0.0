package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

public class ZonaDTO {

    private ZonaDTO() {
    }

    /** Sirve tanto para crear como para actualizar una zona. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;

        @NotBlank(message = "El distrito es obligatorio")
        private String distrito;

        @NotNull(message = "El costo de envío es obligatorio")
        @DecimalMin(value = "0.0", message = "El costo de envío no puede ser negativo")
        private BigDecimal costoEnvio;

        @NotNull(message = "El tiempo estimado es obligatorio")
        @Min(value = 1, message = "El tiempo estimado debe ser al menos 1 minuto")
        private Integer tiempoEstimadoMinutos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String nombre;
        private String distrito;
        private BigDecimal costoEnvio;
        private int tiempoEstimadoMinutos;
        private boolean activa;
        private Date creadoEn;
    }
}
