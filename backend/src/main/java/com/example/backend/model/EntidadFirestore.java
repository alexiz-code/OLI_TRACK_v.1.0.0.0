package com.example.backend.model;

/**
 * Contrato mínimo que deben cumplir las entidades guardadas en Firestore,
 * para que el repositorio genérico pueda leer y asignar el id del documento
 * sin usar reflexión.
 */
public interface EntidadFirestore {
    String getId();
    void setId(String id);
}
