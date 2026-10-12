package com.example.backend.service;

import com.example.backend.dto.AuditDTO;
import com.example.backend.model.AuditLog;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.security.SecurityContextUtils;
import com.example.backend.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

/**
 * Registro de actividad y cambios (HU-022, HU-023). Registrar nunca debe romper
 * la operación de negocio que lo invoca: cualquier error al auditar se ignora.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Mono<Void> registrar(String usuarioId, String accion, String entidad, String entidadId, String detalle) {
        AuditLog log = new AuditLog();
        log.setUsuarioId(usuarioId);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setDetalle(detalle);
        return auditLogRepository.save(log).then().onErrorResume(e -> Mono.empty());
    }

    /** Registra usando el usuario autenticado de la petición actual ("sistema" si no hay). */
    public Mono<Void> registrarActual(String accion, String entidad, String entidadId, String detalle) {
        return SecurityContextUtils.usuarioActual()
                .map(UsuarioAutenticado::getUid)
                .defaultIfEmpty("sistema")
                .flatMap(uid -> registrar(uid, accion, entidad, entidadId, detalle));
    }

    public Flux<AuditDTO.Response> listar(String entidad) {
        Flux<AuditLog> base = StringUtils.hasText(entidad)
                ? auditLogRepository.findByField("entidad", entidad)
                : auditLogRepository.findAll();
        return base.collectList()
                .flatMapMany(lista -> Flux.fromIterable(lista.stream()
                        .sorted(Comparator.comparing(AuditLog::getCreadoEn, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList()))
                .map(l -> new AuditDTO.Response(l.getId(), l.getUsuarioId(), l.getAccion(), l.getEntidad(),
                        l.getEntidadId(), l.getDetalle(), l.getCreadoEn()));
    }
}
