package com.example.backend.repository;

import com.example.backend.model.Faq;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class FaqRepository extends FirestoreRepository<Faq> {

    public FaqRepository(Firestore firestore) {
        super(firestore, Colecciones.FAQS, Faq.class);
    }
}
