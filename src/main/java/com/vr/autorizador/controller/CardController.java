package com.vr.autorizador.controller;

import com.vr.autorizador.controller.dto.CardRequestDTO;
import com.vr.autorizador.controller.dto.CardResponseDTO;
import com.vr.autorizador.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cartoes")
@RequiredArgsConstructor
@Tag(name = "Cartoes", description = "Criacao de cartoes e consulta de saldo")
public class CardController {

    private final CardService cardService;

    /*Apesar do contrato estar para ser retornado a senha na response, acredito que não seja uma boa prática pois pode haver vazamento
    * portanto foi criado um objeto diferente para response da requisição*/
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cria um novo cartao com saldo inicial de R$ 500,00",
            description = "Responde 201 com o numero do cartao. Caso o cartao ja exista, responde 422 com o mesmo corpo.")
    public ResponseEntity<CardResponseDTO> create(@Valid @RequestBody CardRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(request));
    }

    @GetMapping(value = "/{numeroCartao}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtem o saldo do cartao",
            description = "Responde 200 com o saldo. Caso o cartao nao exista, responde 404 sem corpo.")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable("numeroCartao") String cardNumber) {
        return ResponseEntity.ok(cardService.getBalance(cardNumber));
    }
}
