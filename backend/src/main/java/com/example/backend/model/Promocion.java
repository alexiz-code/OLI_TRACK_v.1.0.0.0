package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Documento de la colección "promociones". */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocion implements EntidadFirestore {

    @DocumentId
    private String id;

    private String titulo;
    private String descripcion;
    private Date vigenteDesde;
    private Date vigenteHasta;
    private boolean activa = true;

    @ServerTimestamp
    private Date creadoEn;
}
