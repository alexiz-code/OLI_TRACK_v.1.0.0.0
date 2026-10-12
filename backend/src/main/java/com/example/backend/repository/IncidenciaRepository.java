package com.example.backend.repository;

import com.example.backend.model.EstadoIncidencia;
import com.example.backend.model.Incidencia;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class IncidenciaRepository extends FirestoreRepository<Incidencia> {

    public IncidenciaRepository(Firestore firestore) {
        super(firestore, Colecciones.INCIDENCIAS, Incidencia.class);
    }

    public Flux<Incidencia> findByPedidoId(String pedidoId) {
        return findByField("pedidoId", pedidoId);
    }

    public Flux<Incidencia> findByRepartidorId(String repartidorId) {
        return findByField("repartidorId", repartidorId);
    }

    public Flux<Incidencia> findByEstado(EstadoIncidencia estado) {
        return findByField("estado", estado.name());
    }
}
