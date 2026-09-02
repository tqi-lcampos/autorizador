package com.vr.autorizador.exception;

import com.vr.autorizador.controller.dto.CardResponseDTO;
import lombok.Getter;

@Getter
public class CardAlreadyExistsException extends BusinessException {

    private final CardResponseDTO card;

    public CardAlreadyExistsException(String cardNumber) {
        super("Card %s already exists.".formatted(cardNumber));
        this.card = CardResponseDTO.of(cardNumber);
    }
}
