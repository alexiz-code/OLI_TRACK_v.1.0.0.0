package com.example.backend.service;

import com.example.backend.dto.InventarioDTO;
import com.example.backend.dto.KardexDTO;
import com.example.backend.exception.OperacionInvalidaException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Inventario;
import com.example.backend.repository.InventarioRepository;
import com.example.backend.repository.KardexRepository;
import com.example.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

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
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            return Mono.error(new OperacionInvalidaException("La cantidad debe ser mayor que cero"));
        }
        return inventarioRepository.aplicarMovimientoManual(
                        inventarioId, request.getTipo(), request.getCantidad(), request.getMotivo())
                .onErrorMap(error -> error instanceof IllegalStateException
                                || error instanceof ArithmeticException,
                        error -> error instanceof ArithmeticException
                                ? new OperacionInvalidaException("El movimiento excede el límite permitido")
                                : new OperacionInvalidaException(error.getMessage()))
                .map(mapper::toDTO);
    }

    /** Devuelve stock al inventario (por ejemplo al cancelar un pedido) y deja línea de Kardex. */
    public Mono<Void> reponerPorProducto(String productoId, int cantidad, String motivo) {
        return inventarioRepository.aplicarMovimientoPedido("reposicion-manual-" + UUID.randomUUID(),
                Map.of(productoId, cantidad), false, motivo);
    }

    /** Descuenta stock de forma transaccional y deja una línea de Kardex. */
    public Mono<Void> descontarPorProducto(String productoId, int cantidad) {
        return inventarioRepository.aplicarMovimientoPedido("descuento-manual-" + UUID.randomUUID(),
                Map.of(productoId, cantidad), true, "Descuento automático por pedido");
    }

    /** Reserva o repone todas las líneas de un pedido con validación transaccional en Firestore. */
    public Mono<Void> aplicarMovimientoPedido(String pedidoId, Map<String, Integer> cantidades,
                                              boolean descontar, String motivo) {
        String operacionId = (descontar ? "reserva-" : "reposicion-") + pedidoId;
        return inventarioRepository.aplicarMovimientoPedido(operacionId, cantidades, descontar, motivo)
                .onErrorMap(error -> error instanceof IllegalStateException
                                || error instanceof IllegalArgumentException,
                        error -> new OperacionInvalidaException(error.getMessage()));
    }
}
