package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Documento único (id fijo "empresa") de la colección "configuracion".
 * Datos institucionales, horarios de atención, métodos de pago habilitados y umbrales de alerta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEmpresa implements EntidadFirestore {

    @DocumentId
    private String id;

    private String nombre;
    private String ruc;
    private String direccionCentral;
    private String telefono;
    private String correo;

    private String horarioApertura;
    private String horarioCierre;
    private String diasAtencion;
    /** Si es true, el backend rechaza pedidos fuera del horario de atención. */
    private boolean validarHorarioAtencion;

    private Double latitudCentral;
    private Double longitudCentral;

    private List<MetodoPago> metodosPagoHabilitados = new ArrayList<>();

    private Integer tiempoEstimadoEntregaMinutos;
    /** Minutos desde la creación tras los cuales un pedido activo se considera retrasado. */
    private Integer umbralRetrasoMinutos;
    /** Minutos sin repartidor tras los cuales se alerta "pedido sin asignar". */
    private Integer umbralSinAsignarMinutos;
}
