package com.example.backend.dto;

import com.example.backend.model.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class InventarioDTO {

    private InventarioDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrearRequest {

        @NotBlank(message = "El productoId es obligatorio")
        private String productoId;

        @NotNull(message = "El stock inicial es obligatorio")
        @Min(value = 0, message = "El stock inicial no puede ser negativo")
        private Integer stockActual;

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        private Integer stockMinimo;

        @NotNull(message = "El stock máximo es obligatorio")
        @Min(value = 0, message = "El stock máximo no puede ser negativo")
        private Integer stockMaximo;
    }

    /** Cuerpo para registrar una entrada, salida o ajuste de stock. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimientoRequest {

        @NotNull(message = "El tipo de movimiento es obligatorio")
        private TipoMovimiento tipo;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;

        private String motivo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String productoId;
        private int stockActual;
        private int stockMinimo;
        private int stockMaximo;
        private Date actualizadoEn;
    }
}
