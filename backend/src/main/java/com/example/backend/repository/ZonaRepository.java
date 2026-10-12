package com.example.backend.repository;

import com.example.backend.model.Zona;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class ZonaRepository extends FirestoreRepository<Zona> {

    public ZonaRepository(Firestore firestore) {
        super(firestore, Colecciones.ZONAS, Zona.class);
    }
}
