package com.example.backend.dto;

import com.example.backend.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class KardexDTO {

    private KardexDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String productoId;
        private TipoMovimiento tipoMovimiento;
        private int cantidad;
        private int stockAnterior;
        private int stockNuevo;
        private String motivo;
        private Date creadoEn;
    }
}
