package com.example.backend.repository;

import com.example.backend.model.Promocion;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class PromocionRepository extends FirestoreRepository<Promocion> {

    public PromocionRepository(Firestore firestore) {
        super(firestore, Colecciones.PROMOCIONES, Promocion.class);
    }
}
