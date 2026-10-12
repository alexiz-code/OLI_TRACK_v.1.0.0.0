package com.example.backend.service;

import com.example.backend.dto.FaqDTO;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.model.Faq;
import com.example.backend.repository.FaqRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

/** Preguntas frecuentes (HU-082). */
@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public Mono<FaqDTO.Response> crear(FaqDTO.Request request) {
        Faq f = new Faq();
        aplicar(f, request);
        f.setActiva(true);
        return faqRepository.save(f).map(this::aDTO);
    }

    public Flux<FaqDTO.Response> listarActivas() {
        return ordenar(faqRepository.findByField("activa", true));
    }

    public Flux<FaqDTO.Response> listarTodas() {
        return ordenar(faqRepository.findAll());
    }

    private Flux<FaqDTO.Response> ordenar(Flux<Faq> flux) {
        return flux.collectList()
                .flatMapMany(l -> Flux.fromIterable(l.stream().sorted(Comparator.comparingInt(Faq::getOrden)).toList()))
                .map(this::aDTO);
    }

    public Mono<FaqDTO.Response> actualizar(String id, FaqDTO.Request request) {
        return faqRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("FAQ", id)))
                .flatMap(f -> {
                    aplicar(f, request);
                    return faqRepository.save(f);
                })
                .map(this::aDTO);
    }

    public Mono<Void> cambiarActiva(String id, boolean activa) {
        return faqRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("FAQ", id)))
                .flatMap(f -> {
                    f.setActiva(activa);
                    return faqRepository.save(f);
                })
                .then();
    }

    private void aplicar(Faq f, FaqDTO.Request r) {
        f.setPregunta(r.getPregunta());
        f.setRespuesta(r.getRespuesta());
        f.setOrden(r.getOrden());
    }

    private FaqDTO.Response aDTO(Faq f) {
        return new FaqDTO.Response(f.getId(), f.getPregunta(), f.getRespuesta(), f.getOrden(), f.isActiva());
    }
}
