package com.vr.autorizador.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta da criacao de cartao. Nao expoe a senha; o nome JSON segue o contrato do desafio.
 */
public record CardResponseDTO(

        @JsonProperty("numeroCartao")
        String cardNumber
) {

    public static CardResponseDTO of(String cardNumber) {
        return new CardResponseDTO(cardNumber);
    }
}
