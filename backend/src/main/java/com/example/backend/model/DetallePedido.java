package com.example.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Documento de la colección "detallePedidos".
 * Cada documento es una línea (producto + cantidad) dentro de un pedido.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido implements EntidadFirestore {

    @DocumentId
    private String id;

    /** Referencia al documento en "pedidos". */
    private String pedidoId;

    /** Referencia al documento en "productos". */
    private String productoId;

    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
