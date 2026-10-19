package com.example.backend.service;

import com.example.backend.dto.NotificacionDTO;
import com.example.backend.exception.ApiException;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.Notificacion;
import com.example.backend.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Genera y expone notificaciones internas (persistidas en Firestore). No
 * envía push/email todavía; es la base sobre la que se conectaría FCM más
 * adelante sin cambiar la API expuesta a Angular.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final EntityMapper mapper;

    public NotificacionService(NotificacionRepository notificacionRepository, EntityMapper mapper) {
        this.notificacionRepository = notificacionRepository;
        this.mapper = mapper;
    }

    public Mono<Void> crear(String destinatarioUsuarioId, String titulo, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatarioUsuarioId(destinatarioUsuarioId);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeida(false);
        return notificacionRepository.save(notificacion).then();
    }

    public Flux<NotificacionDTO.Response> listarPropias(String usuarioId) {
        return notificacionRepository.findByDestinatarioUsuarioId(usuarioId).map(mapper::toDTO);
    }

    public Mono<NotificacionDTO.Response> marcarLeida(String id, String usuarioId) {
        return notificacionRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Notificación", id)))
                .flatMap(notificacion -> {
                    if (!usuarioId.equals(notificacion.getDestinatarioUsuarioId())) {
                        return Mono.error(new ApiException(HttpStatus.FORBIDDEN,
                                "No puedes modificar una notificación de otro usuario"));
                    }
                    notificacion.setLeida(true);
                    return notificacionRepository.save(notificacion);
                })
                .map(mapper::toDTO);
    }
}
