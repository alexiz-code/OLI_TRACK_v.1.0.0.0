package com.example.backend.service;

import com.example.backend.dto.ClienteDTO;
import com.example.backend.exception.ApiException;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Cliente;
import com.example.backend.model.Pedido;
import com.example.backend.model.Rol;
import com.example.backend.repository.ClienteRepository;
import com.example.backend.repository.PedidoRepository;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Perfil de negocio del cliente (dirección/ubicación del local incluidas).
 * Se crea después de que el usuario ya tiene su cuenta (Parte de Usuario/Auth).
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final EntityMapper mapper;

    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                          PedidoRepository pedidoRepository, EntityMapper mapper) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.mapper = mapper;
    }

    public Mono<ClienteDTO.Response> crear(String usuarioAutenticadoId, ClienteDTO.CrearRequest request) {
        if (!usuarioAutenticadoId.equals(request.getUsuarioId())) {
            return Mono.error(new ApiException(
                    HttpStatus.FORBIDDEN,
                    "El perfil solo se puede crear para el usuario autenticado"));
        }
        return usuarioRepository.findById(request.getUsuarioId())
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Usuario", request.getUsuarioId())))
                .flatMap(usuario -> {
                    if (usuario.getRol() != Rol.CLIENTE) {
                        return Mono.error(new OperacionInvalidaException(
                                "El usuario indicado no tiene rol CLIENTE"));
                    }
                    return clienteRepository.findByUsuarioId(usuario.getId());
                })
                .flatMap(existente -> Mono.<ClienteDTO.Response>error(
                        new OperacionInvalidaException("Este usuario ya tiene un perfil de cliente")))
                .switchIfEmpty(Mono.defer(() ->
                        clienteRepository.save(mapper.toModel(request)).map(mapper::toDTO)));
    }

    /** HU-057: el cliente guarda más de una dirección de entrega. */
    public Mono<ClienteDTO.Response> agregarDireccion(String clienteId, ClienteDTO.DireccionCrearRequest request) {
        return clienteRepository.findById(clienteId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", clienteId)))
                .flatMap(cliente -> {
                    if (cliente.getDireccionesAdicionales() == null) {
                        cliente.setDireccionesAdicionales(new java.util.ArrayList<>());
                    }
                    cliente.getDireccionesAdicionales().add(mapper.toModel(request));
                    return clienteRepository.save(cliente);
                })
                .map(mapper::toDTO);
    }

    public Mono<ClienteDTO.Response> eliminarDireccion(String clienteId, String direccionId) {
        return clienteRepository.findById(clienteId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", clienteId)))
                .flatMap(cliente -> {
                    if (cliente.getDireccionesAdicionales() != null) {
                        cliente.getDireccionesAdicionales().removeIf(d -> direccionId.equals(d.getId()));
                    }
                    return clienteRepository.save(cliente);
                })
                .map(mapper::toDTO);
    }

    public Mono<ClienteDTO.Response> obtenerPorId(String id) {
        return clienteRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", id)))
                .map(mapper::toDTO);
    }

    public Mono<ClienteDTO.Response> obtenerPorUsuarioId(String usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Perfil de cliente para el usuario", usuarioId)))
                .map(mapper::toDTO);
    }

    public Flux<ClienteDTO.Response> listar() {
        return pedidoRepository.findAll()
                .filter(pedido -> pedido.getClienteId() != null && pedido.getCreadoEn() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Pedido::getClienteId,
                        Pedido::getCreadoEn,
                        (anterior, reciente) -> anterior.after(reciente) ? anterior : reciente))
                .flatMapMany(ultimosPedidos -> clienteRepository.findAll().map(cliente -> {
                    ClienteDTO.Response response = mapper.toDTO(cliente);
                    response.setUltimoPedidoEn(ultimosPedidos.get(cliente.getId()));
                    return response;
                }));
    }

    public Mono<ClienteDTO.Response> actualizar(String id, ClienteDTO.ActualizarRequest request) {
        return clienteRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", id)))
                .flatMap(cliente -> {
                    cliente.setNombreNegocio(request.getNombreNegocio());
                    cliente.setDireccion(request.getDireccion());
                    if (StringUtils.hasText(request.getReferencia())) {
                        cliente.setReferencia(request.getReferencia());
                    }
                    if (request.getUbicacion() != null) {
                        cliente.setUbicacion(mapper.toModel(request.getUbicacion()));
                    }
                    return clienteRepository.save(cliente);
                })
                .map(mapper::toDTO);
    }

    public Mono<Void> desactivar(String id) {
        return clienteRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", id)))
                .flatMap(cliente -> {
                    cliente.setActivo(false);
                    return clienteRepository.save(cliente);
                })
                .then();
    }

    public Mono<Void> reactivar(String id) {
        return clienteRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Cliente", id)))
                .flatMap(cliente -> {
                    cliente.setActivo(true);
                    return clienteRepository.save(cliente);
                })
                .then();
    }
}
