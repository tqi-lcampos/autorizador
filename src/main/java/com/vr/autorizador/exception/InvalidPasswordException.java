package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.DeclineReason;

public class InvalidPasswordException extends TransactionDeclinedException {

    public InvalidPasswordException() {
        super(DeclineReason.INVALID_PASSWORD);
    }
}
