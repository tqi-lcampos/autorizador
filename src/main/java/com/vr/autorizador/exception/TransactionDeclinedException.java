package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.DeclineReason;
import lombok.Getter;

@Getter
public abstract class TransactionDeclinedException extends BusinessException {

    private final DeclineReason reason;

    protected TransactionDeclinedException(DeclineReason reason) {
        super(reason.getCode());
        this.reason = reason;
    }
}
