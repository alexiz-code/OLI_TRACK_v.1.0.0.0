package com.example.backend.service;

import com.example.backend.dto.ProductoDTO;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Inventario;
import com.example.backend.model.Producto;
import com.example.backend.repository.InventarioRepository;
import com.example.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final AuditService auditService;
    private final EntityMapper mapper;

    public ProductoService(ProductoRepository productoRepository, InventarioRepository inventarioRepository,
                            AuditService auditService, EntityMapper mapper) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    public Mono<ProductoDTO.Response> crear(ProductoDTO.CrearRequest request) {
        return productoRepository.save(mapper.toModel(request))
                .flatMap(p -> auditService.registrarActual("PRODUCTO_CREADO", "producto", p.getId(), p.getNombre()).thenReturn(p))
                .map(mapper::toDTO);
    }

    public Mono<ProductoDTO.Response> obtenerPorId(String id) {
        return productoRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Producto", id)))
                .flatMap(p -> conStock(Flux.just(p)).next());
    }

    /** Catálogo completo (uso administrativo). */
    public Flux<ProductoDTO.Response> listar() {
        return conStock(productoRepository.findAll());
    }

    /** Catálogo visible para clientes: solo productos activos, con su disponibilidad (HU-060). */
    public Flux<ProductoDTO.Response> listarActivos() {
        return conStock(productoRepository.findByActivoTrue());
    }

    public Flux<ProductoDTO.Response> listarPorCategoria(String categoria) {
        return conStock(productoRepository.findByCategoria(categoria));
    }

    /** Une cada producto con su stock actual del inventario. */
    private Flux<ProductoDTO.Response> conStock(Flux<Producto> productos) {
        return inventarioRepository.findAll()
                .collectMap(Inventario::getProductoId, Inventario::getStockActual)
                .flatMapMany((Map<String, Integer> stock) -> productos.map(p -> {
                    ProductoDTO.Response dto = mapper.toDTO(p);
                    dto.setStockDisponible(stock.getOrDefault(p.getId(), 0));
                    return dto;
                }));
    }

    public Mono<ProductoDTO.Response> actualizar(String id, ProductoDTO.ActualizarRequest request) {
        return productoRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Producto", id)))
                .flatMap(producto -> {
                    producto.setNombre(request.getNombre());
                    producto.setDescripcion(request.getDescripcion());
                    producto.setCategoria(request.getCategoria());
                    producto.setPrecioUnitario(request.getPrecioUnitario());
                    producto.setUnidadMedida(request.getUnidadMedida());
                    producto.setImagenUrl(request.getImagenUrl());
                    return productoRepository.save(producto);
                })
                .flatMap(p -> auditService.registrarActual("PRODUCTO_ACTUALIZADO", "producto", p.getId(),
                        p.getNombre() + " · precio " + p.getPrecioUnitario()).thenReturn(p))
                .map(mapper::toDTO);
    }

    /** Baja lógica: el producto deja de aparecer en el catálogo pero no se borra. */
    public Mono<Void> desactivar(String id) {
        return cambiarActivo(id, false, "PRODUCTO_DESACTIVADO");
    }

    public Mono<Void> reactivar(String id) {
        return cambiarActivo(id, true, "PRODUCTO_REACTIVADO");
    }

    private Mono<Void> cambiarActivo(String id, boolean activo, String accion) {
        return productoRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Producto", id)))
                .flatMap(producto -> {
                    producto.setActivo(activo);
                    return productoRepository.save(producto);
                })
                .flatMap(p -> auditService.registrarActual(accion, "producto", p.getId(), p.getNombre()))
                .then();
    }
}
