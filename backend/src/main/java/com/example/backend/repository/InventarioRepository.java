package com.example.backend.repository;

import com.example.backend.model.Inventario;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class InventarioRepository extends FirestoreRepository<Inventario> {

    public InventarioRepository(Firestore firestore) {
        super(firestore, Colecciones.INVENTARIOS, Inventario.class);
    }

    /** Cada producto tiene, a lo sumo, un documento de inventario. */
    public Mono<Inventario> findByProductoId(String productoId) {
        return findByField("productoId", productoId).next();
    }
}
