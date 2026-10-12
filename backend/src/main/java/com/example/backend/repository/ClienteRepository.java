package com.example.backend.repository;

import com.example.backend.model.Cliente;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class ClienteRepository extends FirestoreRepository<Cliente> {

    public ClienteRepository(Firestore firestore) {
        super(firestore, Colecciones.CLIENTES, Cliente.class);
    }

    public Mono<Cliente> findByUsuarioId(String usuarioId) {
        return findByField("usuarioId", usuarioId).next();
    }
}
