package com.example.backend.repository;

import com.example.backend.model.AuditLog;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository extends FirestoreRepository<AuditLog> {

    public AuditLogRepository(Firestore firestore) {
        super(firestore, Colecciones.AUDITORIA, AuditLog.class);
    }
}
