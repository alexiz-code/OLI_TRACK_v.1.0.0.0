package com.example.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de error uniforme devuelto por toda la API ante cualquier fallo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String mensaje;
    private final String path;
    private final List<CampoError> errores;

    public ErrorResponse(int status, String error, String mensaje, String path) {
        this(status, error, mensaje, path, null);
    }

    public ErrorResponse(int status, String error, String mensaje, String path, List<CampoError> errores) {
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
        this.errores = errores;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getPath() {
        return path;
    }

    public List<CampoError> getErrores() {
        return errores;
    }

    /** Un error de validación puntual sobre un campo del body. */
    public record CampoError(String campo, String mensaje) {
    }
}
