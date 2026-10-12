package com.example.backend.service;

import com.example.backend.dto.DetallePedidoDTO;
import com.example.backend.dto.IncidenciaDTO;
import com.example.backend.dto.PedidoDTO;
import com.example.backend.exception.ApiException;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Calificacion;
import com.example.backend.model.ConfiguracionEmpresa;
import com.example.backend.model.DetallePedido;
import com.example.backend.model.EstadoPedido;
import com.example.backend.model.EstadoRepartidor;
import com.example.backend.model.HistorialEstado;
import com.example.backend.model.MetodoPago;
import com.example.backend.model.Pedido;
import com.example.backend.model.Producto;
import com.example.backend.model.Repartidor;
import com.example.backend.model.Rol;
import com.example.backend.model.TipoIncidencia;
import com.example.backend.repository.ClienteRepository;
import com.example.backend.repository.DetallePedidoRepository;
import com.example.backend.repository.PedidoRepository;
import com.example.backend.repository.ProductoRepository;
import com.example.backend.repository.RepartidorRepository;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.security.UsuarioAutenticado;

// Firebase Storage
// import com.google.cloud.storage.Acl;
// import com.google.cloud.storage.Blob;
// import com.google.cloud.storage.Bucket;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Firebase Storage
// import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

@Service
public class PedidoService {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final RepartidorRepository repartidorRepository;
    private final ClienteRepository clienteRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final ConfiguracionService configuracionService;
    private final IncidenciaService incidenciaService;

    // Firebase Storage
    // private final Bucket bucket;

    private final EntityMapper mapper;

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         ProductoRepository productoRepository,
                         RepartidorRepository repartidorRepository,
                         ClienteRepository clienteRepository,
                         InventarioService inventarioService,
                         NotificacionService notificacionService,
                         ConfiguracionService configuracionService,
                         IncidenciaService incidenciaService,
                         // Firebase Storage
                         // Bucket bucket,
                         EntityMapper mapper) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.repartidorRepository = repartidorRepository;
        this.clienteRepository = clienteRepository;
        this.inventarioService = inventarioService;
        this.notificacionService = notificacionService;
        this.configuracionService = configuracionService;
        this.incidenciaService = incidenciaService;
        // Firebase Storage
        // this.bucket = bucket;
        this.mapper = mapper;
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private Mono<String> uidActual() {
        return SecurityContextUtils.usuarioActual()
                .map(UsuarioAutenticado::getUid)
                .defaultIfEmpty("sistema");
    }

    private void agregarHistorial(Pedido pedido, EstadoPedido estado, String uid, String comentario) {
        if (pedido.getHistorialEstados() == null) {
            pedido.setHistorialEstados(new ArrayList<>());
        }
        pedido.getHistorialEstados().add(new HistorialEstado(estado, new Date(), uid, comentario));
    }

    private String corto(String id) {
        return id != null && id.length() > 6
                ? id.substring(id.length() - 6)
                : String.valueOf(id);
    }

    private Mono<Pedido> buscar(String pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Pedido", pedidoId)));
    }

    private Mono<Repartidor> repartidorDe(String uid) {
        return repartidorRepository.findByUsuarioId(uid)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Perfil de repartidor para el usuario", uid)));
    }

    public Mono<PedidoDTO.Response> crear(String clienteId, PedidoDTO.CrearRequest request) {
        return configuracionService.obtener()
                .flatMap(cfg -> validarPedidoPermitido(cfg, request)
                        .then(Flux.fromIterable(request.getProductos())
                                .flatMap(linea -> resolverLinea(linea)
                                        .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Producto", linea.getProductoId()))))
                                .collectList()
                                .flatMap(lineas -> construirYGuardar(clienteId, request, cfg, lineas))));
    }

    private Mono<Void> validarPedidoPermitido(ConfiguracionEmpresa cfg, PedidoDTO.CrearRequest request) {
        List<MetodoPago> habilitados = cfg.getMetodosPagoHabilitados();
        if (habilitados != null && !habilitados.isEmpty() && !habilitados.contains(request.getMetodoPago())) {
            return Mono.error(new OperacionInvalidaException("El método de pago " + request.getMetodoPago() + " no está habilitado"));
        }
        if (cfg.isValidarHorarioAtencion() && !dentroDeHorario(cfg)) {
            return Mono.error(new OperacionInvalidaException(
                    "Fuera del horario de atención (" + cfg.getHorarioApertura() + " - " + cfg.getHorarioCierre() + ")"
            ));
        }
        return Mono.empty();
    }

    private boolean dentroDeHorario(ConfiguracionEmpresa cfg) {
        try {
            LocalTime ahora = LocalTime.now(ZONA_LIMA);
            LocalTime apertura = LocalTime.parse(cfg.getHorarioApertura());
            LocalTime cierre = LocalTime.parse(cfg.getHorarioCierre());
            if (apertura.isBefore(cierre)) {
                return !ahora.isBefore(apertura) && !ahora.isAfter(cierre);
            }
            return !ahora.isBefore(apertura) || !ahora.isAfter(cierre);
        } catch (Exception e) {
            return true;
        }
    }

    private Mono<PedidoDTO.Response> construirYGuardar(String clienteId,
                                                       PedidoDTO.CrearRequest request,
                                                       ConfiguracionEmpresa cfg,
                                                       List<DetallePedidoDTO> lineas) {
        BigDecimal total = lineas.stream()
                .map(DetallePedidoDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(total);
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setUbicacionEntrega(mapper.toModel(request.getUbicacionEntrega()));
        pedido.setObservaciones(request.getObservaciones());
        pedido.setMetodoPago(request.getMetodoPago());
        pedido.setCostoEnvio(BigDecimal.ZERO);
        pedido.setTiempoEstimadoMinutos(cfg.getTiempoEstimadoEntregaMinutos());

        return uidActual()
                .flatMap(uid -> {
                    agregarHistorial(pedido, EstadoPedido.PENDIENTE, uid, "Pedido registrado");
                    return pedidoRepository.save(pedido);
                })
                .flatMap(guardado -> guardarDetalle(guardado, lineas)
                        .then(descontarStock(lineas))
                        .then(notificarCliente(
                                guardado,
                                "Pedido registrado",
                                "Recibimos tu pedido #" + corto(guardado.getId()) + ". Te avisaremos cuando cambie de estado."
                        ))
                        .thenReturn(mapper.toDTO(guardado, lineas)));
    }

    private Mono<DetallePedidoDTO> resolverLinea(DetallePedidoDTO linea) {
        return productoRepository.findById(linea.getProductoId())
                .filter(Producto::isActivo)
                .map(producto -> {
                    BigDecimal subtotal = producto.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(linea.getCantidad()));
                    return new DetallePedidoDTO(
                            null,
                            producto.getId(),
                            linea.getCantidad(),
                            producto.getPrecioUnitario(),
                            subtotal
                    );
                });
    }

    private Mono<Void> guardarDetalle(Pedido pedido, List<DetallePedidoDTO> lineas) {
        return Flux.fromIterable(lineas)
                .flatMap(linea -> {
                    DetallePedido detalle = new DetallePedido();
                    detalle.setPedidoId(pedido.getId());
                    detalle.setProductoId(linea.getProductoId());
                    detalle.setCantidad(linea.getCantidad());
                    detalle.setPrecioUnitario(linea.getPrecioUnitario());
                    detalle.setSubtotal(linea.getSubtotal());
                    return detallePedidoRepository.save(detalle);
                })
                .then();
    }

    private Mono<Void> descontarStock(List<DetallePedidoDTO> lineas) {
        return Flux.fromIterable(lineas)
                .concatMap(linea -> inventarioService.descontarPorProducto(
                        linea.getProductoId(),
                        linea.getCantidad()
                ))
                .then();
    }

    // =====================================================================
    // Lecturas
    // =====================================================================

    public Mono<PedidoDTO.Response> obtenerPorId(String id, String uid, Rol rol) {
        return buscar(id)
                .flatMap(p -> autorizarLectura(p, uid, rol).thenReturn(p))
                .flatMap(this::enriquecer);
    }

    private Mono<Void> autorizarLectura(Pedido pedido, String uid, Rol rol) {
        if (rol == Rol.CLIENTE) {
            return verificarPropietario(pedido, uid);
        }
        if (rol == Rol.REPARTIDOR) {
            return verificarRepartidorAsignado(pedido, uid);
        }
        return Mono.empty();
    }

    public Flux<PedidoDTO.Response> listarFiltrado(EstadoPedido estado, LocalDate desde, LocalDate hasta) {
        Instant inicio = desde != null ? desde.atStartOfDay(ZONA_LIMA).toInstant() : null;
        Instant fin = hasta != null ? hasta.plusDays(1).atStartOfDay(ZONA_LIMA).toInstant() : null;

        Flux<Pedido> base = estado != null
                ? pedidoRepository.findByEstado(estado)
                : pedidoRepository.findAll();

        return ordenados(base.filter(p -> {
            if (inicio == null && fin == null) {
                return true;
            }
            if (p.getCreadoEn() == null) {
                return false;
            }
            Instant t = p.getCreadoEn().toInstant();
            return (inicio == null || !t.isBefore(inicio))
                    && (fin == null || t.isBefore(fin));
        }).flatMap(this::enriquecer));
    }

    public Flux<PedidoDTO.Response> listarPorCliente(String clienteId) {
        return ordenados(pedidoRepository.findByClienteId(clienteId).flatMap(this::enriquecer));
    }

    public Flux<PedidoDTO.Response> listarPorRepartidor(String repartidorId) {
        return ordenados(pedidoRepository.findByRepartidorId(repartidorId).flatMap(this::enriquecer));
    }

    private Flux<PedidoDTO.Response> ordenados(Flux<PedidoDTO.Response> flux) {
        return flux.collectList()
                .flatMapMany(lista -> Flux.fromIterable(
                        lista.stream()
                                .sorted(Comparator.comparing(
                                        PedidoDTO.Response::getCreadoEn,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                ))
                                .toList()
                ));
    }

    private Mono<PedidoDTO.Response> enriquecer(Pedido pedido) {
        return detallePedidoRepository.findByPedidoId(pedido.getId())
                .map(mapper::toDTO)
                .collectList()
                .map(detalle -> mapper.toDTO(pedido, detalle));
    }

    public Mono<PedidoDTO.AlertasResponse> alertas() {
        return configuracionService.obtener()
                .flatMap(cfg -> pedidoRepository.findAll()
                        .collectList()
                        .flatMap(pedidos -> {
                            Instant ahora = Instant.now();
                            Instant limiteRetraso = ahora.minus(Duration.ofMinutes(cfg.getUmbralRetrasoMinutos()));
                            Instant limiteSinAsignar = ahora.minus(Duration.ofMinutes(cfg.getUmbralSinAsignarMinutos()));

                            List<Pedido> activos = pedidos.stream()
                                    .filter(p -> p.getEstado() != EstadoPedido.ENTREGADO
                                            && p.getEstado() != EstadoPedido.CANCELADO)
                                    .filter(p -> p.getCreadoEn() != null)
                                    .toList();

                            List<Pedido> retrasados = activos.stream()
                                    .filter(p -> p.getCreadoEn().toInstant().isBefore(limiteRetraso))
                                    .toList();

                            List<Pedido> sinAsignar = activos.stream()
                                    .filter(p -> !StringUtils.hasText(p.getRepartidorId()))
                                    .filter(p -> p.getCreadoEn().toInstant().isBefore(limiteSinAsignar))
                                    .toList();

                            return Mono.zip(
                                    enriquecerLista(retrasados),
                                    enriquecerLista(sinAsignar)
                            ).map(t -> new PedidoDTO.AlertasResponse(t.getT1(), t.getT2()));
                        }));
    }

    private Mono<List<PedidoDTO.Response>> enriquecerLista(List<Pedido> pedidos) {
        return Flux.fromIterable(pedidos)
                .concatMap(this::enriquecer)
                .collectList();
    }


    public Mono<PedidoDTO.Response> editar(String pedidoId, String uid, PedidoDTO.EditarRequest request) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarPropietario(pedido, uid)
                        .then(Mono.defer(() -> {
                            if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo se puede editar un pedido mientras está PENDIENTE"
                                ));
                            }

                            pedido.setDireccionEntrega(request.getDireccionEntrega());
                            if (request.getUbicacionEntrega() != null) {
                                pedido.setUbicacionEntrega(mapper.toModel(request.getUbicacionEntrega()));
                            }
                            pedido.setObservaciones(request.getObservaciones());
                            return pedidoRepository.save(pedido);
                        })))
                .flatMap(this::enriquecer);
    }

    public Mono<PedidoDTO.Response> cancelarPorCliente(String pedidoId, String uid) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarPropietario(pedido, uid)
                        .then(Mono.defer(() -> {
                            if (pedido.getEstado() != EstadoPedido.PENDIENTE
                                    && pedido.getEstado() != EstadoPedido.CONFIRMADO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo puedes cancelar pedidos PENDIENTE o CONFIRMADO"
                                ));
                            }
                            return cambiarEstado(pedidoId, EstadoPedido.CANCELADO, "Cancelado por el cliente");
                        })));
    }

    // =====================================================================
    // Asignación de repartidores
    // =====================================================================

    public Mono<PedidoDTO.Response> asignarRepartidor(String pedidoId, String repartidorId) {
        return uidActual()
                .flatMap(uid -> buscar(pedidoId)
                        .flatMap(pedido -> {
                            EstadoPedido e = pedido.getEstado();
                            if (e != EstadoPedido.PENDIENTE
                                    && e != EstadoPedido.CONFIRMADO
                                    && e != EstadoPedido.EN_PREPARACION) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo se puede asignar repartidor a pedidos PENDIENTE, CONFIRMADO o EN_PREPARACION"
                                ));
                            }

                            return repartidorRepository.findById(repartidorId)
                                    .switchIfEmpty(Mono.error(
                                            RecursoNoEncontradoException.de("Repartidor", repartidorId)
                                    ))
                                    .flatMap(repartidor -> {
                                        if (!repartidor.isActivo()) {
                                            return Mono.error(new OperacionInvalidaException(
                                                    "El repartidor está desactivado"
                                            ));
                                        }

                                        pedido.setRepartidorId(repartidor.getId());
                                        pedido.setEntregaAceptada(false);

                                        if (pedido.getEstado() == EstadoPedido.PENDIENTE) {
                                            pedido.setEstado(EstadoPedido.CONFIRMADO);
                                            agregarHistorial(
                                                    pedido,
                                                    EstadoPedido.CONFIRMADO,
                                                    uid,
                                                    "Confirmado al asignar repartidor"
                                            );
                                        }

                                        agregarHistorial(
                                                pedido,
                                                pedido.getEstado(),
                                                uid,
                                                "Repartidor asignado: " + corto(repartidor.getId())
                                        );

                                        return pedidoRepository.save(pedido);
                                    });
                        })
                        .flatMap(this::enriquecer));
    }

    public Mono<PedidoDTO.Response> asignarRepartidorAutomatico(String pedidoId) {
        return repartidorRepository.findByEstado(EstadoRepartidor.DISPONIBLE)
                .filter(Repartidor::isActivo)
                .next()
                .switchIfEmpty(Mono.error(new OperacionInvalidaException(
                        "No hay repartidores disponibles en este momento"
                )))
                .flatMap(repartidor -> asignarRepartidor(pedidoId, repartidor.getId()));
    }

    public Mono<PedidoDTO.Response> reasignar(String pedidoId, String nuevoRepartidorId, String comentario) {
        return uidActual()
                .flatMap(uid -> buscar(pedidoId)
                        .flatMap(pedido -> {
                            if (pedido.getEstado() == EstadoPedido.ENTREGADO
                                    || pedido.getEstado() == EstadoPedido.CANCELADO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "No se puede reasignar un pedido finalizado"
                                ));
                            }

                            if (!StringUtils.hasText(pedido.getRepartidorId())) {
                                return Mono.error(new OperacionInvalidaException(
                                        "El pedido aún no tiene repartidor: use asignar"
                                ));
                            }

                            if (nuevoRepartidorId.equals(pedido.getRepartidorId())) {
                                return Mono.error(new OperacionInvalidaException(
                                        "El pedido ya está asignado a ese repartidor"
                                ));
                            }

                            String anterior = pedido.getRepartidorId();

                            return repartidorRepository.findById(nuevoRepartidorId)
                                    .switchIfEmpty(Mono.error(
                                            RecursoNoEncontradoException.de("Repartidor", nuevoRepartidorId)
                                    ))
                                    .flatMap(nuevo -> {
                                        if (!nuevo.isActivo()) {
                                            return Mono.error(new OperacionInvalidaException(
                                                    "El repartidor está desactivado"
                                            ));
                                        }

                                        pedido.setRepartidorId(nuevo.getId());
                                        pedido.setEntregaAceptada(false);

                                        agregarHistorial(
                                                pedido,
                                                pedido.getEstado(),
                                                uid,
                                                "Reasignado de " + corto(anterior) + " a " + corto(nuevo.getId())
                                                        + (StringUtils.hasText(comentario) ? ": " + comentario : "")
                                        );

                                        Mono<Void> ajustes = Mono.empty();

                                        if (pedido.getEstado() == EstadoPedido.EN_CAMINO) {
                                            ajustes = actualizarEstadoRepartidor(
                                                    anterior,
                                                    EstadoRepartidor.DISPONIBLE
                                            ).then().then(
                                                    actualizarEstadoRepartidor(
                                                            nuevo.getId(),
                                                            EstadoRepartidor.EN_RUTA
                                                    ).then()
                                            );
                                        }

                                        return ajustes.then(pedidoRepository.save(pedido));
                                    });
                        })
                        .flatMap(this::enriquecer));
    }


    public Mono<PedidoDTO.Response> cambiarEstado(String pedidoId, EstadoPedido nuevoEstado) {
        return cambiarEstado(pedidoId, nuevoEstado, null);
    }

    public Mono<PedidoDTO.Response> cambiarEstado(String pedidoId, EstadoPedido nuevoEstado, String comentario) {
        return uidActual()
                .flatMap(uid -> buscar(pedidoId)
                        .flatMap(pedido -> validarTransicion(pedido, nuevoEstado))
                        .doOnNext(pedido -> agregarHistorial(pedido, nuevoEstado, uid, comentario))
                        .flatMap(pedido -> aplicarEfectosSecundarios(pedido, nuevoEstado))
                        .flatMap(pedido -> notificarCliente(
                                pedido,
                                "Actualización de tu pedido",
                                "Tu pedido #" + corto(pedido.getId()) + " ahora está " + nuevoEstado.name()
                        ).thenReturn(pedido))
                        .flatMap(this::enriquecer));
    }

    public Mono<PedidoDTO.Response> cambiarEstadoComo(String pedidoId,
                                                      EstadoPedido nuevoEstado,
                                                      String comentario,
                                                      String uid,
                                                      Rol rol) {
        if (rol != Rol.REPARTIDOR) {
            return cambiarEstado(pedidoId, nuevoEstado, comentario);
        }

        if (nuevoEstado != EstadoPedido.EN_CAMINO && nuevoEstado != EstadoPedido.ENTREGADO) {
            return Mono.error(new ApiException(
                    HttpStatus.FORBIDDEN,
                    "El repartidor solo puede marcar EN_CAMINO o ENTREGADO"
            ));
        }

        return buscar(pedidoId)
                .flatMap(pedido -> verificarRepartidorAsignado(pedido, uid)
                        .then(Mono.defer(() -> {
                            if (nuevoEstado == EstadoPedido.EN_CAMINO && !pedido.isEntregaAceptada()) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Primero debes aceptar la entrega"
                                ));
                            }
                            return cambiarEstado(pedidoId, nuevoEstado, comentario);
                        })));
    }

    public Mono<PedidoDTO.Response> rechazar(String pedidoId, String observaciones) {
        return buscar(pedidoId)
                .flatMap(pedido -> {
                    if (pedido.getEstado() != EstadoPedido.PENDIENTE
                            && pedido.getEstado() != EstadoPedido.CONFIRMADO) {
                        return Mono.error(new OperacionInvalidaException(
                                "Solo se puede rechazar un pedido PENDIENTE o CONFIRMADO"
                        ));
                    }

                    return cambiarEstado(
                            pedidoId,
                            EstadoPedido.CANCELADO,
                            "Rechazado por supervisión: " + observaciones
                    );
                });
    }

    private Mono<Pedido> validarTransicion(Pedido pedido, EstadoPedido nuevoEstado) {
        EnumSet<EstadoPedido> permitidos = EnumSet.noneOf(EstadoPedido.class);
        permitidos.addAll(List.of(siguientesEstados(pedido.getEstado())));

        if (!permitidos.contains(nuevoEstado)) {
            return Mono.error(new OperacionInvalidaException(
                    "No se puede pasar de " + pedido.getEstado() + " a " + nuevoEstado
            ));
        }

        pedido.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoPedido.ENTREGADO) {
            pedido.setEntregadoEn(new Date());
        }

        return Mono.just(pedido);
    }

    private EstadoPedido[] siguientesEstados(EstadoPedido actual) {
        return switch (actual) {
            case PENDIENTE -> new EstadoPedido[]{
                    EstadoPedido.CONFIRMADO,
                    EstadoPedido.CANCELADO
            };
            case CONFIRMADO -> new EstadoPedido[]{
                    EstadoPedido.EN_PREPARACION,
                    EstadoPedido.CANCELADO
            };
            case EN_PREPARACION -> new EstadoPedido[]{
                    EstadoPedido.EN_CAMINO,
                    EstadoPedido.CANCELADO
            };
            case EN_CAMINO -> new EstadoPedido[]{
                    EstadoPedido.ENTREGADO,
                    EstadoPedido.ENTREGA_FALLIDA
            };
            case ENTREGA_FALLIDA -> new EstadoPedido[]{
                    EstadoPedido.EN_CAMINO,
                    EstadoPedido.CANCELADO
            };
            case ENTREGADO, CANCELADO -> new EstadoPedido[]{};
        };
    }

    private Mono<Pedido> aplicarEfectosSecundarios(Pedido pedido, EstadoPedido nuevoEstado) {
        return pedidoRepository.save(pedido)
                .flatMap(guardado -> {
                    Mono<Void> efectos = Mono.empty();

                    if (nuevoEstado == EstadoPedido.CANCELADO) {
                        efectos = reponerStock(guardado);
                    }

                    if (StringUtils.hasText(guardado.getRepartidorId())) {
                        EstadoRepartidor estadoRepartidor = switch (nuevoEstado) {
                            case EN_CAMINO -> EstadoRepartidor.EN_RUTA;
                            case ENTREGADO, ENTREGA_FALLIDA, CANCELADO -> EstadoRepartidor.DISPONIBLE;
                            default -> null;
                        };

                        if (estadoRepartidor != null) {
                            efectos = efectos.then(
                                    actualizarEstadoRepartidor(
                                            guardado.getRepartidorId(),
                                            estadoRepartidor
                                    ).then()
                            );
                        }
                    }

                    return efectos.thenReturn(guardado);
                });
    }

    private Mono<Void> reponerStock(Pedido pedido) {
        return detallePedidoRepository.findByPedidoId(pedido.getId())
                .concatMap(d -> inventarioService.reponerPorProducto(
                        d.getProductoId(),
                        d.getCantidad(),
                        "Reposición por cancelación del pedido #" + corto(pedido.getId())
                ))
                .then();
    }

    private Mono<Repartidor> actualizarEstadoRepartidor(String repartidorId, EstadoRepartidor estado) {
        return repartidorRepository.findById(repartidorId)
                .flatMap(repartidor -> {
                    repartidor.setEstado(estado);
                    return repartidorRepository.save(repartidor);
                });
    }

    private Mono<Void> notificarCliente(Pedido pedido, String titulo, String mensaje) {
        if (!StringUtils.hasText(pedido.getClienteId())) {
            return Mono.empty();
        }

        return clienteRepository.findById(pedido.getClienteId())
                .flatMap(cliente -> notificacionService.crear(
                        cliente.getUsuarioId(),
                        titulo,
                        mensaje
                ))
                .onErrorResume(e -> Mono.empty())
                .then();
    }


    public Mono<PedidoDTO.Response> aceptarEntrega(String pedidoId, String uid) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarRepartidorAsignado(pedido, uid)
                        .then(Mono.defer(() -> {
                            if (pedido.getEstado() != EstadoPedido.CONFIRMADO
                                    && pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo puedes aceptar entregas CONFIRMADAS o EN_PREPARACION"
                                ));
                            }

                            pedido.setEntregaAceptada(true);
                            agregarHistorial(
                                    pedido,
                                    pedido.getEstado(),
                                    uid,
                                    "Entrega aceptada por el repartidor"
                            );

                            return pedidoRepository.save(pedido);
                        })))
                .flatMap(this::enriquecer);
    }

    public Mono<PedidoDTO.Response> registrarLlegada(String pedidoId, String uid) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarRepartidorAsignado(pedido, uid)
                        .then(Mono.defer(() -> {
                            if (pedido.getEstado() != EstadoPedido.EN_CAMINO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo puedes marcar llegada en un pedido EN_CAMINO"
                                ));
                            }

                            pedido.setLlegadaEn(new Date());
                            agregarHistorial(
                                    pedido,
                                    pedido.getEstado(),
                                    uid,
                                    "El repartidor llegó al destino"
                            );

                            return pedidoRepository.save(pedido)
                                    .flatMap(p -> notificarCliente(
                                            p,
                                            "Tu repartidor llegó",
                                            "Tu pedido #" + corto(p.getId()) + " está en la puerta."
                                    ).thenReturn(p));
                        })))
                .flatMap(this::enriquecer);
    }

    public Mono<PedidoDTO.Response> registrarEntregaFallida(String pedidoId,
                                                            String uid,
                                                            PedidoDTO.EntregaFallidaRequest request) {
        return repartidorDe(uid)
                .flatMap(repartidor -> buscar(pedidoId)
                        .flatMap(pedido -> {
                            if (!repartidor.getId().equals(pedido.getRepartidorId())) {
                                return Mono.error(new ApiException(
                                        HttpStatus.FORBIDDEN,
                                        "Este pedido no está asignado al repartidor autenticado"
                                ));
                            }

                            if (pedido.getEstado() != EstadoPedido.EN_CAMINO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo se puede registrar entrega fallida de un pedido EN_CAMINO"
                                ));
                            }

                            TipoIncidencia tipo = request.getTipo() != null
                                    ? request.getTipo()
                                    : TipoIncidencia.OTRO;

                            return cambiarEstado(
                                    pedidoId,
                                    EstadoPedido.ENTREGA_FALLIDA,
                                    request.getMotivo()
                            ).flatMap(respuesta -> incidenciaService.registrar(
                                    repartidor.getId(),
                                    new IncidenciaDTO.CrearRequest(
                                            pedidoId,
                                            tipo,
                                            "Entrega fallida: " + request.getMotivo()
                                    )
                            ).thenReturn(respuesta));
                        }));
    }

    public Mono<PedidoDTO.Response> calificar(String pedidoId,
                                              String uidSolicitante,
                                              PedidoDTO.CalificarRequest request) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarPropietario(pedido, uidSolicitante)
                        .then(Mono.defer(() -> {
                            if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "Solo se puede calificar un pedido ya ENTREGADO"
                                ));
                            }

                            pedido.setCalificacion(new Calificacion(
                                    request.getEstrellas(),
                                    request.getComentario(),
                                    new Date()
                            ));

                            return pedidoRepository.save(pedido);
                        })))
                .flatMap(this::enriquecer);
    }

    // =====================================================================
    // Firebase Storage deshabilitado temporalmente
    // =====================================================================

    public Mono<PedidoDTO.Response> guardarEvidencia(String pedidoId,
                                                     String uidSolicitante,
                                                     byte[] contenido,
                                                     String contentType,
                                                     String nombreArchivo) {
        return Mono.error(new OperacionInvalidaException(
                "Firebase Storage está deshabilitado temporalmente"
        ));
    }

    /*
    public Mono<PedidoDTO.Response> guardarEvidencia(String pedidoId,
                                                     String uidSolicitante,
                                                     byte[] contenido,
                                                     String contentType,
                                                     String nombreArchivo) {
        return buscar(pedidoId)
                .flatMap(pedido -> verificarPropietario(pedido, uidSolicitante)
                        .then(Mono.defer(() -> {
                            if (pedido.getMetodoPago() == MetodoPago.EFECTIVO) {
                                return Mono.error(new OperacionInvalidaException(
                                        "El pago en efectivo no requiere comprobante"
                                ));
                            }

                            return subirArchivo(
                                    pedidoId,
                                    contenido,
                                    contentType,
                                    nombreArchivo
                            ).flatMap(url -> {
                                pedido.setEvidenciaUrl(url);
                                return pedidoRepository.save(pedido);
                            });
                        })))
                .flatMap(this::enriquecer);
    }

    private Mono<String> subirArchivo(String pedidoId,
                                      byte[] contenido,
                                      String contentType,
                                      String nombreArchivo) {
        String ruta = "evidencias/" + pedidoId + "/" + System.currentTimeMillis() + "-" + nombreArchivo;

        return Mono.fromCallable(() -> {
            Blob blob = bucket.create(ruta, contenido, contentType);
            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
            return "https://storage.googleapis.com/" + bucket.getName() + "/" + ruta;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    */

    private Mono<Void> verificarPropietario(Pedido pedido, String uidSolicitante) {
        return clienteRepository.findById(pedido.getClienteId())
                .switchIfEmpty(Mono.error(new OperacionInvalidaException(
                        "Cliente del pedido no encontrado"
                )))
                .flatMap(cliente -> {
                    if (!cliente.getUsuarioId().equals(uidSolicitante)) {
                        return Mono.error(new ApiException(
                                HttpStatus.FORBIDDEN,
                                "Este pedido no pertenece al usuario autenticado"
                        ));
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> verificarRepartidorAsignado(Pedido pedido, String uid) {
        return repartidorRepository.findByUsuarioId(uid)
                .filter(r -> r.getId().equals(pedido.getRepartidorId()))
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.FORBIDDEN,
                        "Este pedido no está asignado al repartidor autenticado"
                )))
                .then();
    }
}