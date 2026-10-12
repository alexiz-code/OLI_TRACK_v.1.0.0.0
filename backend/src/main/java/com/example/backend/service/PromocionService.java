package com.example.backend.service;

import com.example.backend.dto.PromocionDTO;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.model.Promocion;
import com.example.backend.repository.PromocionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

/** Promociones visibles para el cliente (HU-081). */
@Service
public class PromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionService(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    public Mono<PromocionDTO.Response> crear(PromocionDTO.Request request) {
        Promocion p = new Promocion();
        aplicar(p, request);
        p.setActiva(true);
        return promocionRepository.save(p).map(this::aDTO);
    }

    /** Solo promociones activas y vigentes hoy (para clientes y visitantes). */
    public Flux<PromocionDTO.Response> listarVigentes() {
        Date ahora = new Date();
        return promocionRepository.findByField("activa", true)
                .filter(p -> (p.getVigenteDesde() == null || !p.getVigenteDesde().after(ahora))
                        && (p.getVigenteHasta() == null || !p.getVigenteHasta().before(ahora)))
                .map(this::aDTO);
    }

    public Flux<PromocionDTO.Response> listarTodas() {
        return promocionRepository.findAll().map(this::aDTO);
    }

    public Mono<PromocionDTO.Response> actualizar(String id, PromocionDTO.Request request) {
        return promocionRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Promoción", id)))
                .flatMap(p -> {
                    aplicar(p, request);
                    return promocionRepository.save(p);
                })
                .map(this::aDTO);
    }

    public Mono<Void> cambiarActiva(String id, boolean activa) {
        return promocionRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Promoción", id)))
                .flatMap(p -> {
                    p.setActiva(activa);
                    return promocionRepository.save(p);
                })
                .then();
    }

    private void aplicar(Promocion p, PromocionDTO.Request r) {
        p.setTitulo(r.getTitulo());
        p.setDescripcion(r.getDescripcion());
        p.setVigenteDesde(r.getVigenteDesde());
        p.setVigenteHasta(r.getVigenteHasta());
    }

    private PromocionDTO.Response aDTO(Promocion p) {
        return new PromocionDTO.Response(p.getId(), p.getTitulo(), p.getDescripcion(),
                p.getVigenteDesde(), p.getVigenteHasta(), p.isActiva(), p.getCreadoEn());
    }
}
