package com.vr.autorizador.controller;

import com.vr.autorizador.controller.dto.TransactionRequestDTO;
import com.vr.autorizador.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
@Tag(name = "Transacoes", description = "Autorizacao de transacoes")
public class TransactionController {

    private static final String SUCCESS_BODY = "OK";

    private final AuthorizationService authorizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Autoriza uma transacao",
            description = "Responde 201 OK quando autorizada; 422 com SALDO_INSUFICIENTE, "
                    + "SENHA_INVALIDA ou CARTAO_INEXISTENTE quando alguma regra barra a transacao.")
    public ResponseEntity<String> authorize(
            @Valid @RequestBody TransactionRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Header ausente ou em branco vira null: sem chave enviada pelo cliente nao existe
        // como reconhecer um reenvio, e gerar uma chave aqui apenas disfarcaria essa ausencia.
        String key = idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null;

        try {
            authorizationService.authorize(request, key);
        } catch (AuthorizationService.TransactionAlreadyProcessedException exception) {
              return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_BODY);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_BODY);
    }
}
