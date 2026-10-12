package com.example.backend.repository;

import com.example.backend.model.ConfiguracionEmpresa;
import com.example.backend.util.Colecciones;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class ConfiguracionEmpresaRepository extends FirestoreRepository<ConfiguracionEmpresa> {

    public ConfiguracionEmpresaRepository(Firestore firestore) {
        super(firestore, Colecciones.CONFIGURACION, ConfiguracionEmpresa.class);
    }
}
