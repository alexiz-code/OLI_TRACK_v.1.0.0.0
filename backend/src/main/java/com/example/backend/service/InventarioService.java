package com.example.backend.service;

import com.example.backend.dto.InventarioDTO;
import com.example.backend.dto.KardexDTO;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Inventario;
import com.example.backend.model.Kardex;
import com.example.backend.model.TipoMovimiento;
import com.example.backend.repository.InventarioRepository;
import com.example.backend.repository.KardexRepository;
import com.example.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final KardexRepository kardexRepository;
    private final EntityMapper mapper;

    public InventarioService(InventarioRepository inventarioRepository, ProductoRepository productoRepository,
                              KardexRepository kardexRepository, EntityMapper mapper) {
        this.inventarioRepository = inventarioRepository;
        this.productoRepository = productoRepository;
        this.kardexRepository = kardexRepository;
        this.mapper = mapper;
    }

    public Mono<InventarioDTO.Response> crear(InventarioDTO.CrearRequest request) {
        return productoRepository.findById(request.getProductoId())
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Producto", request.getProductoId())))
                .flatMap(producto -> inventarioRepository.findByProductoId(producto.getId()))
                .flatMap(existente -> Mono.<InventarioDTO.Response>error(
                        new OperacionInvalidaException("Este producto ya tiene un registro de inventario")))
                .switchIfEmpty(Mono.defer(() -> {
                    Inventario inventario = new Inventario();
                    inventario.setProductoId(request.getProductoId());
                    inventario.setStockActual(request.getStockActual());
                    inventario.setStockMinimo(request.getStockMinimo());
                    inventario.setStockMaximo(request.getStockMaximo());
                    return inventarioRepository.save(inventario).map(mapper::toDTO);
                }));
    }

    public Mono<InventarioDTO.Response> obtenerPorProductoId(String productoId) {
        return inventarioRepository.findByProductoId(productoId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Inventario para el producto", productoId)))
                .map(mapper::toDTO);
    }

    public Flux<InventarioDTO.Response> listar() {
        return inventarioRepository.findAll().map(mapper::toDTO);
    }

    /** Productos por debajo de su stock mínimo: útil para un dashboard de alertas. */
    public Flux<InventarioDTO.Response> listarBajoMinimo() {
        return inventarioRepository.findAll()
                .filter(inv -> inv.getStockActual() < inv.getStockMinimo())
                .map(mapper::toDTO);
    }

    /** Historial completo de movimientos de un producto (trazabilidad, no editable). */
    public Flux<KardexDTO.Response> obtenerKardex(String productoId) {
        return kardexRepository.findByProductoId(productoId).map(mapper::toDTO);
    }

    /**
     * Aplica un movimiento de stock y dej a una línea de Kardex con el detalle
     * (stock anterior, stock nuevo, motivo). Es la única forma correcta de
     * cambiar stockActual: nunca se edita directamente.
     */
    public Mono<InventarioDTO.Response> registrarMovimiento(String inventarioId, InventarioDTO.MovimientoRequest request) {
        return inventarioRepository.findById(inventarioId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Inventario", inventarioId)))
                .flatMap(inventario -> {
                    int stockAnterior = inventario.getStockActual();
                    return aplicarMovimiento(inventario, request)
                            .flatMap(inventarioRepository::save)
                            .flatMap(actualizado -> registrarKardex(actualizado.getProductoId(), request.getTipo(),
                                    request.getCantidad(), stockAnterior, actualizado.getStockActual(), request.getMotivo())
                                    .thenReturn(actualizado));
                })
                .map(mapper::toDTO);
    }

    private Mono<Inventario> aplicarMovimiento(Inventario inventario, InventarioDTO.MovimientoRequest request) {
        int nuevoStock = switch (request.getTipo()) {
            case ENTRADA -> inventario.getStockActual() + request.getCantidad();
            case SALIDA -> inventario.getStockActual() - request.getCantidad();
            case AJUSTE -> request.getCantidad();
        };

        if (nuevoStock < 0) {
            return Mono.error(new OperacionInvalidaException(
                    "El movimiento dejaría el stock en negativo (actual: " + inventario.getStockActual() +
                            ", solicitado: " + request.getCantidad() + ")"));
        }

        inventario.setStockActual(nuevoStock);
        return Mono.just(inventario);
    }

    private Mono<Kardex> registrarKardex(String productoId, TipoMovimiento tipo, int cantidad,
                                          int stockAnterior, int stockNuevo, String motivo) {
        Kardex kardex = new Kardex();
        kardex.setProductoId(productoId);
        kardex.setTipoMovimiento(tipo);
        kardex.setCantidad(cantidad);
        kardex.setStockAnterior(stockAnterior);
        kardex.setStockNuevo(stockNuevo);
        kardex.setMotivo(motivo);
        return kardexRepository.save(kardex);
    }

    /** Devuelve stock al inventario (por ejemplo al cancelar un pedido) y deja línea de Kardex. */
    public Mono<Void> reponerPorProducto(String productoId, int cantidad, String motivo) {
        return inventarioRepository.findByProductoId(productoId)
                .flatMap(inventario -> {
                    int stockAnterior = inventario.getStockActual();
                    int nuevoStock = stockAnterior + cantidad;
                    inventario.setStockActual(nuevoStock);
                    return inventarioRepository.save(inventario)
                            .flatMap(actualizado -> registrarKardex(productoId, TipoMovimiento.ENTRADA, cantidad,
                                    stockAnterior, nuevoStock, motivo));
                })
                .then();
    }

    /** Descuenta stock al confirmar un pedido, y deja su propia línea de Kardex. */
    public Mono<Void> descontarPorProducto(String productoId, int cantidad) {
        return inventarioRepository.findByProductoId(productoId)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Inventario para el producto", productoId)))
                .flatMap(inventario -> {
                    int stockAnterior = inventario.getStockActual();
                    int nuevoStock = stockAnterior - cantidad;
                    if (nuevoStock < 0) {
                        return Mono.error(new OperacionInvalidaException(
                                "Stock insuficiente para el producto " + productoId));
                    }
                    inventario.setStockActual(nuevoStock);
                    return inventarioRepository.save(inventario)
                            .flatMap(actualizado -> registrarKardex(productoId, TipoMovimiento.SALIDA, cantidad,
                                    stockAnterior, nuevoStock, "Descuento automático por pedido"));
                })
                .then();
    }
}
