package com.fiap.autorizador.exception;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Set;

public record ErrorMessage(
        int status,
        String error,
        Set<String> messages,
        OffsetDateTime timestamp
) {

    public static ErrorMessage of(HttpStatus status, String message) {
        return of(status, Set.of(message));
    }

    public static ErrorMessage of(HttpStatus status, Set<String> messages) {
        return new ErrorMessage(status.value(), status.getReasonPhrase(), messages, OffsetDateTime.now());
    }
}
