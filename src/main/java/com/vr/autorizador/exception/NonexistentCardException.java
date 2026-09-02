package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.DeclineReason;

public class NonexistentCardException extends TransactionDeclinedException {

    public NonexistentCardException() {
        super(DeclineReason.NONEXISTENT_CARD);
    }
}
