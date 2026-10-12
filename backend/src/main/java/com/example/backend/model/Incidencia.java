package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Documento de la colección "incidencias". */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia implements EntidadFirestore {

    @DocumentId
    private String id;

    private String pedidoId;
    private String repartidorId;
    private TipoIncidencia tipo;
    private String descripcion;
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;

    @ServerTimestamp
    private Date creadoEn;

    private Date cerradoEn;
}
