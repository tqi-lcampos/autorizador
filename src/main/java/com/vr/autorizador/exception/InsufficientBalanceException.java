package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.DeclineReason;

public class InsufficientBalanceException extends TransactionDeclinedException {

    public InsufficientBalanceException() {
        super(DeclineReason.INSUFFICIENT_BALANCE);
    }
}
