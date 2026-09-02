package com.fiap.cfontes0estapar.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Garagem lotada, setor fechado ou entrada duplicada: conflito com o estado atual.
     */
    @ExceptionHandler({GarageFullException.class, SectorClosedException.class, DuplicatedEntryException.class,
            SpotOccupiedException.class})
    public ResponseEntity<ErrorMessage> handleConflict(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage.of(HttpStatus.CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler({SessionNotFoundException.class, SpotNotFoundException.class, SectorNotFoundException.class})
    public ResponseEntity<ErrorMessage> handleNotFound(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(InvalidEventException.class)
    public ResponseEntity<ErrorMessage> handleInvalidEvent(InvalidEventException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.of(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                 HttpHeaders headers,
                                                                 HttpStatusCode status,
                                                                 WebRequest request) {
        Set<String> messages = exception.getBindingResult().getAllErrors().stream()
                .map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "campo invalido"))
                .collect(Collectors.toSet());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.of(HttpStatus.BAD_REQUEST, messages));
    }

    // Fallback para qualquer erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception exception) {
        logger.error("Erro inesperado ao processar a requisicao", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorMessage.of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisicao."));
    }
}
