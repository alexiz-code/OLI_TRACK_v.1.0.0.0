package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Documento de la colección "auditoria": quién hizo qué y cuándo. Solo se crea, nunca se edita. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog implements EntidadFirestore {

    @DocumentId
    private String id;

    private String usuarioId;
    private String accion;
    private String entidad;
    private String entidadId;
    private String detalle;

    @ServerTimestamp
    private Date creadoEn;
}
