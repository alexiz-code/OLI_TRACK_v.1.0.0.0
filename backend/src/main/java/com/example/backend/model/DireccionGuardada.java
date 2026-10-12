package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dirección adicional de entrega de un cliente. Embebida dentro del documento Cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionGuardada {
    private String id;
    private String alias;
    private String direccion;
    private String referencia;
    private Ubicacion ubicacion;
}
