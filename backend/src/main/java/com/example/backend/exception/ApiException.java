package com.example.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de OLITRACK. Lleva el HttpStatus que debe devolver la API,
 * para que el manejador global (Parte 6) no tenga que adivinarlo.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
