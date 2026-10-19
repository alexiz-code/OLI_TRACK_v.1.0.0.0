package com.example.backend.service;

import com.example.backend.dto.IncidenciaDTO;
import com.example.backend.exception.ApiException;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.EstadoIncidencia;
import com.example.backend.model.Incidencia;
import com.example.backend.repository.IncidenciaRepository;
import com.example.backend.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final PedidoRepository pedidoRepository;
    private final EntityMapper mapper;

    public IncidenciaService(IncidenciaRepository incidenciaRepository, PedidoRepository pedidoRepository, EntityMapper mapper) {
        this.incidenciaRepository = incidenciaRepository;
        this.pedidoRepository = pedidoRepository;
        this.mapper = mapper;
    }

    /** Solo el repartidor asignado al pedido puede registrar una incidencia sobre él. */
    public Mono<IncidenciaDTO.Response> registrar(String repartidorId, IncidenciaDTO.CrearRequest request) {
        return pedidoRepository.findById(request.getPedidoId())
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Pedido", request.getPedidoId())))
                .flatMap(pedido -> {
                    if (!repartidorId.equals(pedido.getRepartidorId())) {
                        return Mono.error(new OperacionInvalidaException(
                                "Solo el repartidor asignado puede registrar una incidencia de este pedido"));
                    }
                    return incidenciaRepository.save(mapper.toModel(request, repartidorId));
                })
                .map(mapper::toDTO);
    }

    /** El supervisor puede registrar una incidencia sobre cualquier pedido (HU-043). */
    public Mono<IncidenciaDTO.Response> registrarComoSupervisor(IncidenciaDTO.CrearRequest request) {
        return pedidoRepository.findById(request.getPedidoId())
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Pedido", request.getPedidoId())))
                .flatMap(pedido -> incidenciaRepository.save(mapper.toModel(request, pedido.getRepartidorId())))
                .map(mapper::toDTO);
    }

    public Mono<IncidenciaDTO.Response> obtenerPorId(String id, String repartidorId) {
        return incidenciaRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Incidencia", id)))
                .flatMap(incidencia -> {
                    if (repartidorId != null && !repartidorId.equals(incidencia.getRepartidorId())) {
                        return Mono.error(new ApiException(HttpStatus.FORBIDDEN,
                                "No tienes acceso a esta incidencia"));
                    }
                    return Mono.just(incidencia);
                })
                .map(mapper::toDTO);
    }

    public Flux<IncidenciaDTO.Response> listar() {
        return incidenciaRepository.findAll().map(mapper::toDTO);
    }

    public Flux<IncidenciaDTO.Response> listarPorPedido(String pedidoId, String repartidorId) {
        Mono<Void> acceso = repartidorId == null
                ? Mono.empty()
                : pedidoRepository.findById(pedidoId)
                        .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Pedido", pedidoId)))
                        .flatMap(pedido -> repartidorId.equals(pedido.getRepartidorId())
                                ? Mono.empty()
                                : Mono.error(new ApiException(HttpStatus.FORBIDDEN,
                                        "No tienes acceso a las incidencias de este pedido")))
                        .then();
        return acceso.thenMany(incidenciaRepository.findByPedidoId(pedidoId)
                .filter(incidencia -> repartidorId == null
                        || repartidorId.equals(incidencia.getRepartidorId()))
                .map(mapper::toDTO));
    }

    public Flux<IncidenciaDTO.Response> listarPorRepartidor(String repartidorId) {
        return incidenciaRepository.findByRepartidorId(repartidorId).map(mapper::toDTO);
    }

    public Mono<IncidenciaDTO.Response> cambiarEstado(String id, EstadoIncidencia estado) {
        return incidenciaRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Incidencia", id)))
                .flatMap(incidencia -> {
                    incidencia.setEstado(estado);
                    if (estado == EstadoIncidencia.CERRADA) {
                        incidencia.setCerradoEn(new Date());
                    }
                    return incidenciaRepository.save(incidencia);
                })
                .map(mapper::toDTO);
    }
}
