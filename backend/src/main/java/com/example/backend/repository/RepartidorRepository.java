package com.example.backend.repository;

import com.example.backend.model.EstadoRepartidor;
import com.example.backend.model.Repartidor;
import com.example.backend.model.Ubicacion;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
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

    /** Reclama de forma atómica un repartidor libre para impedir asignaciones simultáneas. */
    public Mono<Repartidor> reclamarDisponible(String repartidorId) {
        DocumentReference ref = collection().document(repartidorId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("No existe el repartidor " + repartidorId);
            }
            Repartidor repartidor = snapshot.toObject(Repartidor.class);
            if (repartidor == null || !repartidor.isActivo()
                    || repartidor.getEstado() != EstadoRepartidor.DISPONIBLE) {
                throw new IllegalStateException("El repartidor no está activo y disponible");
            }
            transaction.update(ref, "estado", EstadoRepartidor.EN_RUTA.name());
            repartidor.setEstado(EstadoRepartidor.EN_RUTA);
            return repartidor;
        }));
    }

    public Mono<Void> actualizarEstado(String repartidorId, EstadoRepartidor estado) {
        DocumentReference ref = collection().document(repartidorId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("No existe el repartidor " + repartidorId);
            }
            transaction.update(ref, "estado", estado.name());
            return null;
        })).then();
    }

    public Mono<Void> actualizarPlaca(String repartidorId, String placa) {
        DocumentReference ref = collection().document(repartidorId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("No existe el repartidor " + repartidorId);
            }
            transaction.update(ref, "placaVehiculo", placa);
            return null;
        })).then();
    }

    public Mono<Void> actualizarUbicacion(String repartidorId, Ubicacion ubicacion) {
        DocumentReference ref = collection().document(repartidorId);
        return com.example.backend.util.ReactiveFirebase.defer(() -> firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("No existe el repartidor " + repartidorId);
            }
            transaction.update(ref, "ubicacionActual", ubicacion);
            return null;
        })).then();
    }
}
