package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

public class ProductoDTO {

    private ProductoDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrearRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                max = 120,
                message = "El nombre no debe superar 120 caracteres"
        )
        private String nombre;

        private String descripcion;

        @NotBlank(message = "La categoría es obligatoria")
        private String categoria;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "El precio debe ser mayor a 0"
        )
        private BigDecimal precioUnitario;

        @NotBlank(message = "La unidad de medida es obligatoria")
        private String unidadMedida;

        private String imagenUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActualizarRequest {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                max = 120,
                message = "El nombre no debe superar 120 caracteres"
        )
        private String nombre;

        private String descripcion;

        @NotBlank(message = "La categoría es obligatoria")
        private String categoria;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "El precio debe ser mayor a 0"
        )
        private BigDecimal precioUnitario;

        @NotBlank(message = "La unidad de medida es obligatoria")
        private String unidadMedida;

        private String imagenUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private String id;

        private String nombre;

        private String descripcion;

        private String categoria;

        private BigDecimal precioUnitario;

        private String unidadMedida;

        private String imagenUrl;

        private boolean activo;

        private Date creadoEn;

        /**
         * Stock actual según Inventario.
         * 0 si todavía no existe inventario.
         */
        private Integer stockDisponible;
    }
}