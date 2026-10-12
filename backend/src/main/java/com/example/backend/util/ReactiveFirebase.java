package com.example.backend.util;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Puente entre el Firebase Admin SDK (basado en ApiFuture) y Project Reactor.
 *
 * El SDK de Firebase es asíncrono pero no reactivo: devuelve ApiFuture. Esta clase
 * registra un callback sobre esa future y emite el resultado en un Mono, de modo que
 * nunca se invoca .get() ni .block() dentro de un flujo reactivo.
 */
public final class ReactiveFirebase {

    private ReactiveFirebase() {
    }

    /**
     * Convierte una ApiFuture en un Mono. El callback se ejecuta en el hilo que
     * completa la future (directExecutor), sin ocupar hilos del event loop.
     */
    public static <T> Mono<T> fromFuture(ApiFuture<T> future) {
        return Mono.create(sink -> ApiFutures.addCallback(future, new ApiFutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                if (result == null) {
                    sink.success();
                } else {
                    sink.success(result);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                sink.error(t);
            }
        }, MoreExecutors.directExecutor()));
    }

    /**
     * Igual que {@link #fromFuture(ApiFuture)} pero difiere la creación de la future
     * hasta que haya una suscripción. Es la forma correcta de usarlo: evita que la
     * llamada a Firestore se dispare al construir el pipeline.
     */
    public static <T> Mono<T> defer(Callable<ApiFuture<T>> supplier) {
        return Mono.defer(() -> {
            try {
                return fromFuture(supplier.call());
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Convierte una ApiFuture que devuelve una colección en un Flux de sus elementos.
     */
    public static <T> Flux<T> deferMany(Callable<ApiFuture<? extends Collection<T>>> supplier) {
        return Mono.defer(() -> {
            try {
                @SuppressWarnings("unchecked")
                ApiFuture<Collection<T>> future = (ApiFuture<Collection<T>>) supplier.call();
                return fromFuture(future);
            } catch (Exception e) {
                return Mono.<Collection<T>>error(e);
            }
        }).flatMapIterable(collection -> collection);
    }
}
