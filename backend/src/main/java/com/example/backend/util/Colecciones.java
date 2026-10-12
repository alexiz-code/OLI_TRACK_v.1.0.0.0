package com.example.backend.util;

/**
 * Nombres de las colecciones de Firestore usadas por OLITRACK.
 * Centralizarlos evita errores de tipeo dispersos por los repositorios.
 */
public final class Colecciones {

    public static final String USUARIOS = "usuarios";
    public static final String CLIENTES = "clientes";
    public static final String REPARTIDORES = "repartidores";
    public static final String PRODUCTOS = "productos";
    public static final String INVENTARIOS = "inventarios";
    public static final String PEDIDOS = "pedidos";
    public static final String DETALLE_PEDIDOS = "detallePedidos";
    public static final String INCIDENCIAS = "incidencias";
    public static final String KARDEX = "kardex";
    public static final String CONSULTAS = "consultas";
    public static final String NOTIFICACIONES = "notificaciones";
    public static final String ZONAS = "zonas";
    public static final String CONFIGURACION = "configuracion";
    public static final String AUDITORIA = "auditoria";
    public static final String PROMOCIONES = "promociones";
    public static final String FAQS = "faqs";

    private Colecciones() {
    }
}
