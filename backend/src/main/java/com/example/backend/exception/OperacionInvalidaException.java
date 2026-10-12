package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class OperacionInvalidaException extends ApiException {

    public OperacionInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
