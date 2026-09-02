package com.fiap.autorizador.exception;

import com.fiap.autorizador.controller.dto.CartaoRequestDTO;
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

    @ExceptionHandler(CartaoExistenteException.class)
    public ResponseEntity<CartaoRequestDTO> handleCartaoExistente(CartaoExistenteException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(exception.getCartao());
    }

    @ExceptionHandler(CartaoNaoEncontradoException.class)
    public ResponseEntity<Void> handleCartaoNaoEncontrado(CartaoNaoEncontradoException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(TransacaoRecusadaException.class)
    public ResponseEntity<String> handleTransacaoRecusada(TransacaoRecusadaException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(exception.getMotivo().name());
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
        logger.error("Erro inesperado ao processar a requisicao", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorMessage.of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisicao."));
    }
}
