package com.example.backend.controller;

import com.example.backend.dto.PedidoDTO;
import com.example.backend.model.EstadoPedido;
import com.example.backend.model.Rol;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.security.UsuarioAutenticado;
import com.example.backend.service.ClienteService;
import com.example.backend.service.PedidoService;
import com.example.backend.service.RepartidorService;
import jakarta.validation.Valid;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.web.bind.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final RepartidorService repartidorService;

    public PedidoController(PedidoService pedidoService, ClienteService clienteService, RepartidorService repartidorService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.repartidorService = repartidorService;
    }

    // ---------- Creación y consulta ----------

    /** El clienteId nunca se toma del body: siempre del perfil del usuario autenticado. */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ResponseEntity<PedidoDTO.Response>> crear(@Valid @RequestBody PedidoDTO.CrearRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()))
                .flatMap(cliente -> pedidoService.crear(cliente.getId(), request))
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Flux<PedidoDTO.Response> listar(@RequestParam(required = false) EstadoPedido estado,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return pedidoService.listarFiltrado(estado, desde, hasta);
    }

    /** HU-048 / HU-049: pedidos retrasados y sin asignar. */
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.AlertasResponse> alertas() {
        return pedidoService.alertas();
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    public Flux<PedidoDTO.Response> misPedidos() {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> clienteService.obtenerPorUsuarioId(actual.getUid()))
                .flatMapMany(cliente -> pedidoService.listarPorCliente(cliente.getId()));
    }

    @GetMapping("/mis-entregas")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public Flux<PedidoDTO.Response> misEntregas() {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> repartidorService.obtenerPorUsuarioId(actual.getUid()))
                .flatMapMany(repartidor -> pedidoService.listarPorRepartidor(repartidor.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR', 'CLIENTE', 'REPARTIDOR')")
    public Mono<PedidoDTO.Response> obtenerPorId(@PathVariable String id) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.obtenerPorId(id, actual.getUid(), actual.getRol()));
    }

    /** El cliente corrige dirección/ubicación/observaciones mientras el pedido siga PENDIENTE. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<PedidoDTO.Response> editar(@PathVariable String id, @Valid @RequestBody PedidoDTO.EditarRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.editar(id, actual.getUid(), request));
    }

    /** HU-074: el cliente cancela su propio pedido. */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<PedidoDTO.Response> cancelar(@PathVariable String id) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.cancelarPorCliente(id, actual.getUid()));
    }

    // ---------- Asignación (administrador / supervisor) ----------

    @PatchMapping("/{id}/repartidor")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.Response> asignarRepartidor(@PathVariable String id,
                                                        @Valid @RequestBody PedidoDTO.AsignarRepartidorRequest request) {
        return pedidoService.asignarRepartidor(id, request.getRepartidorId());
    }

    @PatchMapping("/{id}/repartidor/auto")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.Response> asignarRepartidorAutomatico(@PathVariable String id) {
        return pedidoService.asignarRepartidorAutomatico(id);
    }

    /** HU-033. */
    @PatchMapping("/{id}/repartidor/reasignar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.Response> reasignar(@PathVariable String id, @Valid @RequestBody PedidoDTO.ReasignarRequest request) {
        return pedidoService.reasignar(id, request.getRepartidorId(), request.getComentario());
    }

    /** HU-029: validar (confirmar) o HU-030: rechazar. */
    @PatchMapping("/{id}/validar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.Response> validar(@PathVariable String id) {
        return pedidoService.cambiarEstado(id, EstadoPedido.CONFIRMADO, "Validado por supervisión");
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')")
    public Mono<PedidoDTO.Response> rechazar(@PathVariable String id, @Valid @RequestBody PedidoDTO.RechazarRequest request) {
        return pedidoService.rechazar(id, request.getObservaciones());
    }

    /** Avanza el pedido en su ciclo de vida. Admin/Supervisor: cualquier transición válida.
     *  Repartidor: solo EN_CAMINO/ENTREGADO sobre lo que tiene asignado. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISOR', 'REPARTIDOR')")
    public Mono<PedidoDTO.Response> cambiarEstado(@PathVariable String id, @Valid @RequestBody PedidoDTO.CambiarEstadoRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.cambiarEstadoComo(id, request.getEstado(), request.getComentario(),
                        actual.getUid(), actual.getRol()));
    }

    // ---------- Acciones del repartidor ----------

    @PatchMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public Mono<PedidoDTO.Response> aceptarEntrega(@PathVariable String id) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.aceptarEntrega(id, actual.getUid()));
    }

    @PatchMapping("/{id}/llegada")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public Mono<PedidoDTO.Response> registrarLlegada(@PathVariable String id) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.registrarLlegada(id, actual.getUid()));
    }

    @PatchMapping("/{id}/entrega-fallida")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public Mono<PedidoDTO.Response> entregaFallida(@PathVariable String id, @Valid @RequestBody PedidoDTO.EntregaFallidaRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap((UsuarioAutenticado actual) -> pedidoService.registrarEntregaFallida(id, actual.getUid(), request));
    }

    // ---------- Calificación y comprobante (cliente) ----------

    @PostMapping("/{id}/calificacion")
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<PedidoDTO.Response> calificar(@PathVariable String id, @Valid @RequestBody PedidoDTO.CalificarRequest request) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> pedidoService.calificar(id, actual.getUid(), request));
    }

    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<PedidoDTO.Response> subirEvidencia(@PathVariable String id, @RequestPart("archivo") FilePart archivo) {
        return SecurityContextUtils.usuarioActual()
                .flatMap(actual -> DataBufferUtils.join(archivo.content())
                        .map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            return bytes;
                        })
                        .flatMap(bytes -> {
                            String contentType = archivo.headers().getContentType() != null
                                    ? archivo.headers().getContentType().toString() : "application/octet-stream";
                            return pedidoService.guardarEvidencia(id, actual.getUid(), bytes, contentType, archivo.filename());
                        }));
    }
}
