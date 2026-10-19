package com.example.backend.service;

import com.example.backend.mapper.EntityMapper;
import com.example.backend.dto.RepartidorDTO;
import com.example.backend.model.EstadoPedido;
import com.example.backend.model.EstadoRepartidor;
import com.example.backend.model.Pedido;
import com.example.backend.model.Repartidor;
import com.example.backend.model.Usuario;
import com.example.backend.repository.ClienteRepository;
import com.example.backend.repository.PedidoRepository;
import com.example.backend.repository.RepartidorRepository;
import com.example.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepartidorServiceTest {

    private RepartidorRepository repartidorRepository;
    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;
    private EntityMapper mapper;
    private RepartidorService repartidorService;

    @BeforeEach
    void setUp() {
        repartidorRepository = mock(RepartidorRepository.class);
        pedidoRepository = mock(PedidoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        mapper = mock(EntityMapper.class);
        repartidorService = new RepartidorService(
                repartidorRepository,
                usuarioRepository,
                mock(ClienteRepository.class),
                pedidoRepository,
                mapper
        );
        when(repartidorRepository.actualizarEstado(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(EstadoRepartidor.class)
        )).thenReturn(reactor.core.publisher.Mono.empty());
    }

    @Test
    void devuelveADisponibleAlRepartidorEnRutaQueNoTienePedidosActivos() {
        Repartidor repartidor = repartidor("r1", EstadoRepartidor.EN_RUTA);
        when(repartidorRepository.findAll()).thenReturn(Flux.just(repartidor));
        when(pedidoRepository.findAll()).thenReturn(Flux.empty());

        repartidorService.sincronizarEstados().block();

        verify(repartidorRepository).actualizarEstado("r1", EstadoRepartidor.DISPONIBLE);
    }

    @Test
    void conservaEnRutaAlRepartidorConUnPedidoActivoAsignado() {
        Repartidor repartidor = repartidor("r1", EstadoRepartidor.DISPONIBLE);
        Pedido pedido = new Pedido();
        pedido.setRepartidorId("r1");
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        when(repartidorRepository.findAll()).thenReturn(Flux.just(repartidor));
        when(pedidoRepository.findAll()).thenReturn(Flux.just(pedido));

        repartidorService.sincronizarEstados().block();

        verify(repartidorRepository).actualizarEstado("r1", EstadoRepartidor.EN_RUTA);
    }

    @Test
    void iniciaDisponibleAlRepartidorSinEntregasActivas() {
        Repartidor repartidor = repartidor("r1", EstadoRepartidor.DESCONECTADO);
        RepartidorDTO.Response response = new RepartidorDTO.Response();
        response.setEstado(EstadoRepartidor.DISPONIBLE);
        when(repartidorRepository.findByUsuarioId("u1"))
                .thenReturn(reactor.core.publisher.Mono.just(repartidor));
        when(pedidoRepository.findByRepartidorId("r1")).thenReturn(Flux.empty());
        when(mapper.toDTO(repartidor)).thenReturn(response);

        repartidorService.prepararInicioSesion("u1").block();

        verify(repartidorRepository).actualizarEstado("r1", EstadoRepartidor.DISPONIBLE);
    }

    @Test
    void mantieneEnRutaAlIniciarSesionSiTieneEntregaActiva() {
        Repartidor repartidor = repartidor("r1", EstadoRepartidor.DESCONECTADO);
        Pedido pedido = new Pedido();
        pedido.setRepartidorId("r1");
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        RepartidorDTO.Response response = new RepartidorDTO.Response();
        response.setEstado(EstadoRepartidor.EN_RUTA);
        when(repartidorRepository.findByUsuarioId("u1"))
                .thenReturn(reactor.core.publisher.Mono.just(repartidor));
        when(pedidoRepository.findByRepartidorId("r1")).thenReturn(Flux.just(pedido));
        when(mapper.toDTO(repartidor)).thenReturn(response);

        repartidorService.prepararInicioSesion("u1").block();

        verify(repartidorRepository).actualizarEstado("r1", EstadoRepartidor.EN_RUTA);
    }

    @Test
    void listaParaAsignacionManualDisponiblesYDesconectadosSinPedidosActivos() {
        Repartidor disponible = repartidor("r1", EstadoRepartidor.DISPONIBLE);
        Repartidor desconectado = repartidor("r2", EstadoRepartidor.DESCONECTADO);
        Repartidor ocupado = repartidor("r3", EstadoRepartidor.EN_RUTA);
        Pedido pedidoActivo = new Pedido();
        pedidoActivo.setRepartidorId("r3");
        pedidoActivo.setEstado(EstadoPedido.CONFIRMADO);
        doReturn(Flux.just(disponible, desconectado, ocupado))
                .when(repartidorRepository).findAll();
        when(pedidoRepository.findAll()).thenReturn(Flux.just(pedidoActivo));
        when(usuarioRepository.findById("ur1")).thenReturn(reactor.core.publisher.Mono.just(usuario("ur1")));
        when(usuarioRepository.findById("ur2")).thenReturn(reactor.core.publisher.Mono.just(usuario("ur2")));
        when(mapper.toDTO(any(Repartidor.class))).thenAnswer(invocation -> {
            Repartidor repartidor = invocation.getArgument(0);
            return response(repartidor.getId(), repartidor.getEstado());
        });

        List<RepartidorDTO.Response> asignables =
                repartidorService.listarAsignablesManualmente().collectList().block();

        assertEquals(List.of("r1", "r2"), asignables.stream().map(RepartidorDTO.Response::getId).toList());
    }

    private Repartidor repartidor(String id, EstadoRepartidor estado) {
        Repartidor repartidor = new Repartidor();
        repartidor.setId(id);
        repartidor.setUsuarioId("u" + id);
        repartidor.setActivo(true);
        repartidor.setEstado(estado);
        return repartidor;
    }

    private Usuario usuario(String id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(id);
        usuario.setEmail(id + "@example.com");
        return usuario;
    }

    private RepartidorDTO.Response response(String id, EstadoRepartidor estado) {
        RepartidorDTO.Response response = new RepartidorDTO.Response();
        response.setId(id);
        response.setEstado(estado);
        return response;
    }
}
