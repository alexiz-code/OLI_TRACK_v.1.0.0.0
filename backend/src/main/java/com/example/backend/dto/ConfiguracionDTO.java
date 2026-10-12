package com.example.backend.dto;

import com.example.backend.model.MetodoPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ConfiguracionDTO {

    private ConfiguracionDTO() {
    }

    /** Se usa igual para leer (GET) y escribir (PUT) la configuración de la empresa. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Datos {

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        private String nombre;

        private String ruc;
        private String direccionCentral;
        private String telefono;
        private String correo;

        @NotBlank(message = "El horario de apertura es obligatorio (HH:mm)")
        private String horarioApertura;

        @NotBlank(message = "El horario de cierre es obligatorio (HH:mm)")
        private String horarioCierre;

        private String diasAtencion;
        private boolean validarHorarioAtencion;

        private Double latitudCentral;
        private Double longitudCentral;

        @NotEmpty(message = "Debe haber al menos un método de pago habilitado")
        private List<MetodoPago> metodosPagoHabilitados;

        @NotNull
        @Min(1)
        private Integer tiempoEstimadoEntregaMinutos;

        @NotNull
        @Min(1)
        private Integer umbralRetrasoMinutos;

        @NotNull
        @Min(1)
        private Integer umbralSinAsignarMinutos;
    }
}
