package com.vr.autorizador.exception;

public class CardNotFoundException extends BusinessException {

    public CardNotFoundException(String cardNumber) {
        super("Card %s not found.".formatted(cardNumber));
    }
}
