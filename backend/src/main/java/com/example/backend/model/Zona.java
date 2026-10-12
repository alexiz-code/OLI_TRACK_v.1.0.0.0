package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/** Documento de la colección "zonas" (zonas de reparto). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Zona implements EntidadFirestore {

    @DocumentId
    private String id;

    private String nombre;
    private String distrito;
    private BigDecimal costoEnvio = BigDecimal.ZERO;
    private int tiempoEstimadoMinutos;
    private boolean activa = true;

    @ServerTimestamp
    private Date creadoEn;
}
