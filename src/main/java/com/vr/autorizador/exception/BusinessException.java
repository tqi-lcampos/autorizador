package com.vr.autorizador.exception;

public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
