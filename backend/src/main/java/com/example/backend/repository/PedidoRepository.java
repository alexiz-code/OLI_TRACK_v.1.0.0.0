package com.example.backend.repository;

import com.example.backend.model.EstadoPedido;
import com.example.backend.model.Pedido;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class PedidoRepository extends FirestoreRepository<Pedido> {

    public PedidoRepository(Firestore firestore) {
        super(firestore, Colecciones.PEDIDOS, Pedido.class);
    }

    public Flux<Pedido> findByClienteId(String clienteId) {
        return findByField("clienteId", clienteId);
    }

    public Flux<Pedido> findByRepartidorId(String repartidorId) {
        return findByField("repartidorId", repartidorId);
    }

    public Flux<Pedido> findByEstado(EstadoPedido estado) {
        return findByField("estado", estado.name());
    }
}
