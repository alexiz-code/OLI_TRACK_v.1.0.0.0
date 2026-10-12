package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** Una línea del historial de un pedido. Embebida dentro del documento Pedido. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {
    private EstadoPedido estado;
    private Date fecha;
    private String usuarioId;
    private String comentario;
}
