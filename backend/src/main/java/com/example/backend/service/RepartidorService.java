package com.example.backend.service;

import com.example.backend.dto.RepartidorDTO;
import com.example.backend.dto.UbicacionDTO;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.EstadoRepartidor;
import com.example.backend.model.Rol;
import com.example.backend.repository.RepartidorRepository;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import com.example.backend.exception.ApiException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RepartidorService {

    private final RepartidorRepository repartidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityMapper mapper;

    public RepartidorService(RepartidorRepository repartidorRepository, UsuarioRepository usuarioRepository, EntityMapper mapper) {
        this.repartidorRepository = repartidorRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    public Mono<RepartidorDTO.Response> crear(RepartidorDTO.CrearRequest request) {
        return usuarioRepository.findById(request.getUsuarioId())
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Usuario", request.getUsuarioId())))
                .flatMap(usuario -> {
                    if (usuario.getRol() != Rol.REPARTIDOR) {
                        return Mono.error(new OperacionInvalidaException(
                                "El usuario indicado no tiene rol REPARTIDOR"));
                    }
                    return repartidorRepository.findByUsuarioId(usuario.getId());
                })
                .flatMap(existente -> Mono.<RepartidorDTO.Response>error(
                        new OperacionInvalidaException("Este usuario ya tiene un perfil de repartidor")))
                .switchIfEmpty(Mono.defer(() ->
                        repartidorRepository.save(mapper.toModel(request)).map(mapper::toDTO)));
    }

    public Mono<RepartidorDTO.Response> obtenerPorId(String id) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .map(mapper::toDTO);
    }

    public Mono<RepartidorDTO.Response> obtenerPorUsuarioId(String usuarioId) {
        return repartidorRepository.findByUsuarioId(usuarioId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Perfil de repartidor para el usuario", usuarioId)))
                .map(mapper::toDTO);
    }

    public Flux<RepartidorDTO.Response> listar() {
        return repartidorRepository.findAll().map(mapper::toDTO);
    }

    /** Repartidores libres para que un administrador o cliente les asigne un pedido. */
    public Flux<RepartidorDTO.Response> listarDisponibles() {
        return repartidorRepository.findByEstado(EstadoRepartidor.DISPONIBLE).map(mapper::toDTO);
    }

    public Mono<RepartidorDTO.Response> actualizar(String id, RepartidorDTO.ActualizarRequest request) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .flatMap(repartidor -> {
                    repartidor.setPlacaVehiculo(request.getPlacaVehiculo());
                    return repartidorRepository.save(repartidor);
                })
                .map(mapper::toDTO);
    }

    public Mono<RepartidorDTO.Response> cambiarEstado(String id, EstadoRepartidor estado) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .flatMap(repartidor -> {
                    repartidor.setEstado(estado);
                    return repartidorRepository.save(repartidor);
                })
                .map(mapper::toDTO);
    }

    /**
     * Actualiza la última ubicación GPS del repartidor. Solo el propio
     * repartidor (dueño del perfil) puede reportar su ubicación.
     */
    public Mono<RepartidorDTO.Response> actualizarUbicacion(String id, String uidSolicitante, UbicacionDTO ubicacion) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .flatMap(repartidor -> {
                    if (!repartidor.getUsuarioId().equals(uidSolicitante)) {
                        return Mono.error(new ApiException(HttpStatus.FORBIDDEN,
                                "Solo el propio repartidor puede reportar su ubicación"));
                    }
                    repartidor.setUbicacionActual(mapper.toModel(ubicacion));
                    return repartidorRepository.save(repartidor);
                })
                .map(mapper::toDTO);
    }

    /** Última ubicación conocida del repartidor (para seguimiento desde Angular). */
    public Mono<UbicacionDTO> obtenerUbicacion(String id) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .map(repartidor -> {
                    if (repartidor.getUbicacionActual() == null) {
                        throw RecursoNoEncontradoException.de("Ubicación del repartidor", id);
                    }
                    return mapper.toDTO(repartidor.getUbicacionActual());
                });
    }

    public Mono<Void> desactivar(String id) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .flatMap(repartidor -> {
                    repartidor.setActivo(false);
                    repartidor.setEstado(EstadoRepartidor.DESCONECTADO);
                    return repartidorRepository.save(repartidor);
                })
                .then();
    }

    public Mono<Void> reactivar(String id) {
        return repartidorRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Repartidor", id)))
                .flatMap(repartidor -> {
                    repartidor.setActivo(true);
                    return repartidorRepository.save(repartidor);
                })
                .then();
    }
}
