package com.vr.autorizador.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransactionRequestDTO(

        @JsonProperty("numeroCartao")
        @NotBlank(message = "numeroCartao e obrigatorio")
        @Pattern(regexp = "\\d{16}", message = "numeroCartao deve conter 16 digitos")
        String cardNumber,

        @JsonProperty("senhaCartao")
        @NotBlank(message = "senhaCartao e obrigatoria")
        String cardPassword,

        @JsonProperty("valor")
        @NotNull(message = "valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
        BigDecimal amount
) {
}
