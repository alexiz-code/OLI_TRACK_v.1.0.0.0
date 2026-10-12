package com.example.backend.repository;

import com.example.backend.model.Consulta;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaRepository extends FirestoreRepository<Consulta> {

    public ConsultaRepository(Firestore firestore) {
        super(firestore, Colecciones.CONSULTAS, Consulta.class);
    }
}
