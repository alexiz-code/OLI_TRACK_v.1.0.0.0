package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Documento de la colección "notificaciones". Una fila por destinatario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion implements EntidadFirestore {

    @DocumentId
    private String id;

    /** uid del usuario destinatario (coincide con el id en "usuarios"). */
    private String destinatarioUsuarioId;

    private String titulo;
    private String mensaje;
    private boolean leida = false;

    @ServerTimestamp
    private Date creadoEn;
}
