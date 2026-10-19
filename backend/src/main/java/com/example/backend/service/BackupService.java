package com.example.backend.service;

import com.example.backend.util.Colecciones;
import com.example.backend.util.ReactiveFirebase;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia de seguridad lógica (HU-024): exporta todas las colecciones a un JSON.
 * Es una exportación de lectura; NO restaura datos (HU-025 se resuelve con los
 * respaldos administrados de Firestore, ver README).
 */
@Service
public class BackupService {

    private static final List<String> COLECCIONES = List.of(
            Colecciones.USUARIOS, Colecciones.CLIENTES, Colecciones.REPARTIDORES, Colecciones.PRODUCTOS,
            Colecciones.INVENTARIOS, Colecciones.PEDIDOS, Colecciones.DETALLE_PEDIDOS, Colecciones.INCIDENCIAS,
            Colecciones.KARDEX, Colecciones.CONSULTAS, Colecciones.NOTIFICACIONES, Colecciones.ZONAS,
            Colecciones.OPERACIONES_STOCK_PEDIDOS, Colecciones.CONFIGURACION, Colecciones.AUDITORIA,
            Colecciones.PROMOCIONES, Colecciones.FAQS);

    private final Firestore firestore;
    private final AuditService auditService;

    public BackupService(Firestore firestore, AuditService auditService) {
        this.firestore = firestore;
        this.auditService = auditService;
    }

    public Mono<Map<String, Object>> exportar() {
        return Flux.fromIterable(COLECCIONES)
                .concatMap(nombre -> ReactiveFirebase.<QuerySnapshot>defer(() -> firestore.collection(nombre).get())
                        .map(snapshot -> {
                            List<Map<String, Object>> docs = new ArrayList<>();
                            snapshot.getDocuments().forEach(d -> {
                                Map<String, Object> fila = new LinkedHashMap<>();
                                fila.put("id", d.getId());
                                fila.put("datos", limpiar(d.getData()));
                                docs.add(fila);
                            });
                            return Map.entry(nombre, (Object) docs);
                        }))
                .collectList()
                .map(entradas -> {
                    Map<String, Object> resultado = new LinkedHashMap<>();
                    resultado.put("generadoEn", java.time.Instant.now().toString());
                    Map<String, Object> colecciones = new LinkedHashMap<>();
                    entradas.forEach(e -> colecciones.put(e.getKey(), e.getValue()));
                    resultado.put("colecciones", colecciones);
                    return resultado;
                })
                .flatMap(r -> auditService.registrarActual("BACKUP_EXPORTADO", "backup", "-", "Exportación completa").thenReturn(r));
    }

    /** Convierte tipos propios de Firestore (Timestamp) a valores serializables en JSON. */
    private Object limpiar(Object valor) {
        if (valor instanceof Timestamp t) {
            return t.toDate().toInstant().toString();
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> copia = new LinkedHashMap<>();
            mapa.forEach((k, v) -> copia.put(String.valueOf(k), limpiar(v)));
            return copia;
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(this::limpiar).toList();
        }
        return valor;
    }
}
