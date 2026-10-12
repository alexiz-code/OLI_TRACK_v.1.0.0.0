package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidoException extends ApiException {

    public TokenInvalidoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
