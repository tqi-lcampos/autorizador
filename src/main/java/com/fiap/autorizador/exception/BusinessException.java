package com.fiap.cfontes0estapar.exception;

/**
 * Base das violacoes de regra de negocio do estacionamento.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
