package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }

    public static RecursoNoEncontradoException de(String entidad, String id) {
        return new RecursoNoEncontradoException(entidad + " no encontrado: " + id);
    }
}
