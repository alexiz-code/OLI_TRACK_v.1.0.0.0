package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Documento de la colección "consultas". Enviada por un visitante sin cuenta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consulta implements EntidadFirestore {

    @DocumentId
    private String id;

    private String nombre;
    private String email;
    private String asunto;
    private String mensaje;
    private EstadoConsulta estado = EstadoConsulta.NUEVA;

    @ServerTimestamp
    private Date creadoEn;
}
