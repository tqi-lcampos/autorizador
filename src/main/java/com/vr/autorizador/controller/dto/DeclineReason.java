package com.vr.autorizador.controller.dto;

public enum DeclineReason {

    INSUFFICIENT_BALANCE("SALDO_INSUFICIENTE"),
    INVALID_PASSWORD("SENHA_INVALIDA"),
    NONEXISTENT_CARD("CARTAO_INEXISTENTE");

    private final String code;

    DeclineReason(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
