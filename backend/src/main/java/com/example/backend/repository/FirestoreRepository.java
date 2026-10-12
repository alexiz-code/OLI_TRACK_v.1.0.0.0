package com.example.backend.repository;

import com.example.backend.model.EntidadFirestore;
import com.example.backend.util.ReactiveFirebase;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo genérico sobre una colección de Firestore.
 *
 * Ninguna operación aquí bloquea el hilo: todo pasa por {@link ReactiveFirebase},
 * que envuelve las ApiFuture del SDK de Firebase en Mono/Flux.
 *
 * @param <T> tipo de entidad (debe implementar EntidadFirestore para exponer su id)
 */
public abstract class FirestoreRepository<T extends EntidadFirestore> {

    protected final Firestore firestore;
    private final String coleccion;
    private final Class<T> tipo;

    protected FirestoreRepository(Firestore firestore, String coleccion, Class<T> tipo) {
        this.firestore = firestore;
        this.coleccion = coleccion;
        this.tipo = tipo;
    }

    protected CollectionReference collection() {
        return firestore.collection(coleccion);
    }

    /** Busca un documento por id. Vacío si no existe. */
    public Mono<T> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }
        return ReactiveFirebase.<com.google.cloud.firestore.DocumentSnapshot>defer(() -> collection().document(id).get())
                .flatMap(snapshot -> snapshot.exists()
                        ? Mono.justOrEmpty(snapshot.toObject(tipo))
                        : Mono.empty());
    }

    /** Devuelve todos los documentos de la colección. */
    public Flux<T> findAll() {
        return ReactiveFirebase.<QuerySnapshot>defer(collection()::get)
                .flatMapIterable(QuerySnapshot::getDocuments)
                .map(doc -> doc.toObject(tipo));
    }

    /** Busca documentos donde `campo` sea igual a `valor`. */
    public Flux<T> findByField(String campo, Object valor) {
        return ReactiveFirebase.<QuerySnapshot>defer(() -> collection().whereEqualTo(campo, valor).get())
                .flatMapIterable(QuerySnapshot::getDocuments)
                .map(doc -> doc.toObject(tipo));
    }

    /** Punto de extensión para que las subclases arme queries más elaboradas (orderBy, límites, etc.). */
    protected Flux<T> ejecutar(Query query) {
        return ReactiveFirebase.<QuerySnapshot>defer(query::get)
                .flatMapIterable(QuerySnapshot::getDocuments)
                .map(doc -> doc.toObject(tipo));
    }

    /**
     * Crea o actualiza. Si la entidad no trae id, Firestore genera uno nuevo
     * antes de escribir, para que el objeto devuelto ya lo incluya.
     */
    public Mono<T> save(T entity) {
        return Mono.defer(() -> {
            DocumentReference ref = StringUtils.hasText(entity.getId())
                    ? collection().document(entity.getId())
                    : collection().document();

            entity.setId(ref.getId());

            return ReactiveFirebase.<WriteResult>defer(() -> ref.set(entity))
                    .thenReturn(entity);
        });
    }

    public Mono<Void> deleteById(String id) {
        return ReactiveFirebase.<WriteResult>defer(() -> collection().document(id).delete())
                .then();
    }

    public Mono<Boolean> existsById(String id) {
        return findById(id).hasElement();
    }
}
