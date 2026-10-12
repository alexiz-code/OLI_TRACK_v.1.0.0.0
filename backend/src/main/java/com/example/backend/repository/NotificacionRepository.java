package com.example.backend.repository;

import com.example.backend.model.Notificacion;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class NotificacionRepository extends FirestoreRepository<Notificacion> {

    public NotificacionRepository(Firestore firestore) {
        super(firestore, Colecciones.NOTIFICACIONES, Notificacion.class);
    }

    public Flux<Notificacion> findByDestinatarioUsuarioId(String usuarioId) {
        return findByField("destinatarioUsuarioId", usuarioId);
    }
}
