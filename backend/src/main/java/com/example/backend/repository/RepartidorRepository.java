package com.example.backend.repository;

import com.example.backend.model.EstadoRepartidor;
import com.example.backend.model.Repartidor;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class RepartidorRepository extends FirestoreRepository<Repartidor> {

    public RepartidorRepository(Firestore firestore) {
        super(firestore, Colecciones.REPARTIDORES, Repartidor.class);
    }

    public Mono<Repartidor> findByUsuarioId(String usuarioId) {
        return findByField("usuarioId", usuarioId).next();
    }

    /** Usado por GET /api/repartidores/disponibles. */
    public Flux<Repartidor> findByEstado(EstadoRepartidor estado) {
        return findByField("estado", estado.name());
    }
}
