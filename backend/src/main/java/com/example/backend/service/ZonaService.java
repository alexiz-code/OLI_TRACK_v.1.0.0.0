package com.example.backend.service;

import com.example.backend.dto.ZonaDTO;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.model.Zona;
import com.example.backend.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Zonas de reparto (HU-016, HU-017, HU-018). */
@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final AuditService auditService;

    public ZonaService(ZonaRepository zonaRepository, AuditService auditService) {
        this.zonaRepository = zonaRepository;
        this.auditService = auditService;
    }

    public Mono<ZonaDTO.Response> crear(ZonaDTO.Request request) {
        Zona zona = new Zona();
        aplicar(zona, request);
        zona.setActiva(true);
        return zonaRepository.save(zona)
                .flatMap(z -> auditService.registrarActual("ZONA_CREADA", "zona", z.getId(), z.getNombre()).thenReturn(z))
                .map(this::aDTO);
    }

    public Flux<ZonaDTO.Response> listar() {
        return zonaRepository.findByField("activa", true).map(this::aDTO);
    }

    public Flux<ZonaDTO.Response> listarTodas() {
        return zonaRepository.findAll().map(this::aDTO);
    }

    public Mono<ZonaDTO.Response> obtenerPorId(String id) {
        return zonaRepository.findById(id)
                .filter(Zona::isActiva)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Zona", id)))
                .map(this::aDTO);
    }

    public Mono<ZonaDTO.Response> actualizar(String id, ZonaDTO.Request request) {
        return zonaRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Zona", id)))
                .flatMap(z -> {
                    aplicar(z, request);
                    return zonaRepository.save(z);
                })
                .flatMap(z -> auditService.registrarActual("ZONA_ACTUALIZADA", "zona", z.getId(), z.getNombre()).thenReturn(z))
                .map(this::aDTO);
    }

    public Mono<Void> cambiarActiva(String id, boolean activa) {
        return zonaRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Zona", id)))
                .flatMap(z -> {
                    z.setActiva(activa);
                    return zonaRepository.save(z);
                })
                .flatMap(z -> auditService.registrarActual(activa ? "ZONA_REACTIVADA" : "ZONA_DESACTIVADA", "zona", z.getId(), z.getNombre()))
                .then();
    }

    private void aplicar(Zona zona, ZonaDTO.Request r) {
        zona.setNombre(r.getNombre());
        zona.setDistrito(r.getDistrito());
        zona.setCostoEnvio(r.getCostoEnvio());
        zona.setTiempoEstimadoMinutos(r.getTiempoEstimadoMinutos());
    }

    private ZonaDTO.Response aDTO(Zona z) {
        return new ZonaDTO.Response(z.getId(), z.getNombre(), z.getDistrito(), z.getCostoEnvio(),
                z.getTiempoEstimadoMinutos(), z.isActiva(), z.getCreadoEn());
    }
}
