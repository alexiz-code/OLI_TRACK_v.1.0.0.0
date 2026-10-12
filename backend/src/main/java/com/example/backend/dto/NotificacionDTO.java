package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class NotificacionDTO {

    private NotificacionDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String titulo;
        private String mensaje;
        private boolean leida;
        private Date creadoEn;
    }
}
