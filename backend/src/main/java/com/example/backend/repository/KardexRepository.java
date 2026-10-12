package com.example.backend.repository;

import com.example.backend.model.Kardex;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class KardexRepository extends FirestoreRepository<Kardex> {

    public KardexRepository(Firestore firestore) {
        super(firestore, Colecciones.KARDEX, Kardex.class);
    }

    public Flux<Kardex> findByProductoId(String productoId) {
        return findByField("productoId", productoId);
    }
}
