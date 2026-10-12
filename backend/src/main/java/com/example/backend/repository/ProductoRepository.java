package com.example.backend.repository;

import com.example.backend.model.Producto;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class ProductoRepository extends FirestoreRepository<Producto> {

    public ProductoRepository(Firestore firestore) {
        super(firestore, Colecciones.PRODUCTOS, Producto.class);
    }

    /** Catálogo visible para clientes: solo productos activos. */
    public Flux<Producto> findByActivoTrue() {
        return findByField("activo", true);
    }

    public Flux<Producto> findByCategoria(String categoria) {
        return findByField("categoria", categoria);
    }
}
