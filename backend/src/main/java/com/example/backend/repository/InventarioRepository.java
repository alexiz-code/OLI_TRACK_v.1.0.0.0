package com.example.backend.repository;

import com.example.backend.model.Inventario;
import com.example.backend.model.Kardex;
import com.example.backend.model.TipoMovimiento;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class InventarioRepository extends FirestoreRepository<Inventario> {

    public InventarioRepository(Firestore firestore) {
        super(firestore, Colecciones.INVENTARIOS, Inventario.class);
    }

    /** Cada producto tiene, a lo sumo, un documento de inventario. */
    public Mono<Inventario> findByProductoId(String productoId) {
        return findByField("productoId", productoId).next();
    }

    /** Aplica en una sola transacción todos los movimientos de inventario de un pedido. */
    public Mono<Void> aplicarMovimientoPedido(String operacionId, Map<String, Integer> cantidades,
                                               boolean descontar, String motivo) {
        if (cantidades.isEmpty()) {
            return Mono.empty();
        }
        if (cantidades.size() > 249) {
            return Mono.error(new IllegalArgumentException("El pedido contiene demasiados productos"));
        }

        DocumentReference operacionRef = firestore.collection(Colecciones.OPERACIONES_STOCK_PEDIDOS)
                .document(operacionId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot operacion = transaction.get(operacionRef).get();
            if (operacion.exists()) {
                return null;
            }

            List<MovimientoPendiente> movimientos = new ArrayList<>();
            for (Map.Entry<String, Integer> entrada : cantidades.entrySet()) {
                String productoId = entrada.getKey();
                int cantidad = entrada.getValue();
                if (cantidad <= 0) {
                    throw new IllegalArgumentException("La cantidad debe ser mayor que cero para " + productoId);
                }

                QuerySnapshot resultado = transaction.get(collection()
                        .whereEqualTo("productoId", productoId)).get();
                if (resultado.isEmpty()) {
                    throw new IllegalStateException("No existe inventario para el producto " + productoId);
                }
                if (resultado.size() > 1) {
                    throw new IllegalStateException("Hay más de un inventario para el producto " + productoId);
                }

                DocumentSnapshot documento = resultado.getDocuments().get(0);
                Inventario inventario = documento.toObject(Inventario.class);
                if (inventario == null) {
                    throw new IllegalStateException("No se pudo leer el inventario del producto " + productoId);
                }
                int anterior = inventario.getStockActual();
                int nuevo = descontar ? anterior - cantidad : anterior + cantidad;
                if (descontar && nuevo < 0) {
                    throw new IllegalStateException("Stock insuficiente para el producto " + productoId
                            + " (disponible: " + anterior + ", solicitado: " + cantidad + ")");
                }
                if (nuevo < 0) {
                    throw new IllegalStateException("El movimiento dejaría stock negativo para " + productoId);
                }
                movimientos.add(new MovimientoPendiente(documento.getReference(), productoId,
                        cantidad, anterior, nuevo));
            }

            // Todas las lecturas ocurren antes de escribir, como exige Firestore.
            for (MovimientoPendiente movimiento : movimientos) {
                transaction.update(movimiento.inventarioRef(), Map.of(
                        "stockActual", movimiento.stockNuevo(),
                        "actualizadoEn", new Date()
                ));

                Kardex kardex = new Kardex();
                kardex.setProductoId(movimiento.productoId());
                kardex.setTipoMovimiento(descontar ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA);
                kardex.setCantidad(movimiento.cantidad());
                kardex.setStockAnterior(movimiento.stockAnterior());
                kardex.setStockNuevo(movimiento.stockNuevo());
                kardex.setMotivo(motivo);
                transaction.set(firestore.collection(Colecciones.KARDEX).document(), kardex);
            }
            transaction.set(operacionRef, Map.of(
                    "tipo", descontar ? "RESERVA" : "REPOSICION",
                    "creadoEn", new Date()
            ));
            return null;
        })).then();
    }

    private record MovimientoPendiente(DocumentReference inventarioRef, String productoId,
                                       int cantidad, int stockAnterior, int stockNuevo) { }

    /** Actualiza un movimiento manual sin perder cambios concurrentes sobre el mismo inventario. */
    public Mono<Inventario> aplicarMovimientoManual(String inventarioId, TipoMovimiento tipo,
                                                     int cantidad, String motivo) {
        DocumentReference inventarioRef = collection().document(inventarioId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(inventarioRef).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("No se encontró el inventario " + inventarioId);
            }
            Inventario inventario = snapshot.toObject(Inventario.class);
            if (inventario == null) {
                throw new IllegalStateException("No se pudo leer el inventario " + inventarioId);
            }
            int anterior = inventario.getStockActual();
            int nuevo = switch (tipo) {
                case ENTRADA -> Math.addExact(anterior, cantidad);
                case SALIDA -> Math.subtractExact(anterior, cantidad);
                case AJUSTE -> cantidad;
            };
            if (nuevo < 0) {
                throw new IllegalStateException("El movimiento dejaría el stock en negativo (actual: "
                        + anterior + ", solicitado: " + cantidad + ")");
            }

            transaction.update(inventarioRef, Map.of(
                    "stockActual", nuevo,
                    "actualizadoEn", new Date()
            ));
            Kardex kardex = new Kardex();
            kardex.setProductoId(inventario.getProductoId());
            kardex.setTipoMovimiento(tipo);
            kardex.setCantidad(cantidad);
            kardex.setStockAnterior(anterior);
            kardex.setStockNuevo(nuevo);
            kardex.setMotivo(motivo);
            transaction.set(firestore.collection(Colecciones.KARDEX).document(UUID.randomUUID().toString()), kardex);
            inventario.setStockActual(nuevo);
            inventario.setActualizadoEn(new Date());
            return inventario;
        }));
    }
}
