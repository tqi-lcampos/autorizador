package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.CardResponseDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<CardResponseDTO> handleCardAlreadyExists(CardAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(exception.getCard());
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<Void> handleCardNotFound(CardNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(TransactionDeclinedException.class)
    public ResponseEntity<String> handleTransactionDeclined(TransactionDeclinedException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(exception.getReason().getCode());
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception exception) {
        logger.error("Unexpected error while processing the request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorMessage.of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisicao."));
    }
}
