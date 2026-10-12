package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Documento de la colección "usuarios".
 * El id coincide con el UID de Firebase Authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements EntidadFirestore {

    @DocumentId
    private String id;

    private String nombre;
    private String email;
    private String telefono;
    private Rol rol;
    private boolean activo = true;

    @ServerTimestamp
    private Date creadoEn;
}
