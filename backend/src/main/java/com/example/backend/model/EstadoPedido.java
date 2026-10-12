package com.example.backend.model;

/** Ciclo de vida de un pedido. */
public enum EstadoPedido {
    PENDIENTE,
    CONFIRMADO,
    EN_PREPARACION,
    EN_CAMINO,
    ENTREGADO,
    ENTREGA_FALLIDA,
    CANCELADO
}
