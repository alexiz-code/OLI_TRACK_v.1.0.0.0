package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class AuditDTO {

    private AuditDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String usuarioId;
        private String accion;
        private String entidad;
        private String entidadId;
        private String detalle;
        private Date creadoEn;
    }
}
