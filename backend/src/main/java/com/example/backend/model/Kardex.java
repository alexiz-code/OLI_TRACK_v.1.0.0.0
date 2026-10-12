package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Documento de la colección "kardex". Cada registro es inmutable: una línea
 * por cada movimiento de stock, para trazabilidad histórica. Nunca se edita,
 * solo se crea.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Kardex implements EntidadFirestore {

    @DocumentId
    private String id;

    private String productoId;
    private TipoMovimiento tipoMovimiento;
    private int cantidad;
    private int stockAnterior;
    private int stockNuevo;
    private String motivo;

    @ServerTimestamp
    private Date creadoEn;
}
