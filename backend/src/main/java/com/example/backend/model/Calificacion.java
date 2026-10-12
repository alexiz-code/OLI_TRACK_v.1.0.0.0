package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Embebido dentro de Pedido; no es colección aparte. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Calificacion {
    private int estrellas;
    private String comentario;
    private Date fecha;
}
