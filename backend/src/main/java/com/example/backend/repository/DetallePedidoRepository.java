package com.example.backend.repository;

import com.example.backend.model.DetallePedido;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.example.backend.util.ReactiveFirebase;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DetallePedidoRepository extends FirestoreRepository<DetallePedido> {

    public DetallePedidoRepository(Firestore firestore) {
        super(firestore, Colecciones.DETALLE_PEDIDOS, DetallePedido.class);
    }

    public Flux<DetallePedido> findByPedidoId(String pedidoId) {
        return findByField("pedidoId", pedidoId);
    }

    /** Elimina todas las líneas de un pedido (usado si se cancela antes de confirmar). */
    public Mono<Void> deleteByPedidoId(String pedidoId) {
        return ReactiveFirebase.<QuerySnapshot>defer(() -> collection().whereEqualTo("pedidoId", pedidoId).get())
                .flatMapIterable(QuerySnapshot::getDocuments)
                .map(QueryDocumentSnapshot::getReference)
                .flatMap(ref -> ReactiveFirebase.defer(ref::delete))
                .then();
    }
}
