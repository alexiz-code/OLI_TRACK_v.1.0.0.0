package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Documento de la colección "faqs" (preguntas frecuentes). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faq implements EntidadFirestore {

    @DocumentId
    private String id;

    private String pregunta;
    private String respuesta;
    private int orden;
    private boolean activa = true;
}
