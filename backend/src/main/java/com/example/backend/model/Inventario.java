package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Documento de la colección "inventarios".
 * Un documento de inventario por producto (relación 1 a 1 vía productoId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventario implements EntidadFirestore {

    @DocumentId
    private String id;

    /** Referencia al documento en "productos". */
    private String productoId;

    private int stockActual;
    private int stockMinimo;
    private int stockMaximo;

    @ServerTimestamp
    private Date actualizadoEn;
}
