package com.example.backend.dto;

import com.example.backend.model.EstadoIncidencia;
import com.example.backend.model.TipoIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class IncidenciaDTO {

    private IncidenciaDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrearRequest {

        @NotBlank(message = "El pedidoId es obligatorio")
        private String pedidoId;

        @NotNull(message = "El tipo de incidencia es obligatorio")
        private TipoIncidencia tipo;

        @NotBlank(message = "La descripción es obligatoria")
        private String descripcion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CambiarEstadoRequest {

        @NotNull(message = "El estado es obligatorio")
        private EstadoIncidencia estado;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String pedidoId;
        private String repartidorId;
        private TipoIncidencia tipo;
        private String descripcion;
        private EstadoIncidencia estado;
        private Date creadoEn;
        private Date cerradoEn;
    }
}
