package com.vr.autorizador.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CardRequestDTO(

        @JsonProperty("numeroCartao")
        @NotBlank(message = "numeroCartao e obrigatorio")
        @Pattern(regexp = "\\d{16}", message = "numeroCartao deve conter 16 digitos")
        String cardNumber,

        @JsonProperty("senha")
        @NotBlank(message = "senha e obrigatoria")
        String password
) {
}
