package com.example.backend.repository;

import com.example.backend.model.Usuario;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UsuarioRepository extends FirestoreRepository<Usuario> {

    public UsuarioRepository(Firestore firestore) {
        super(firestore, Colecciones.USUARIOS, Usuario.class);
    }

    /** Útil para validar duplicados al registrar (aunque el email real vive en Firebase Auth). */
    public Mono<Usuario> findByEmail(String email) {
        return findByField("email", email).next();
    }
}
