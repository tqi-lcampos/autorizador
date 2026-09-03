package com.vr.autorizador.controller;

import com.vr.autorizador.exception.InsufficientBalanceException;
import com.vr.autorizador.exception.InvalidPasswordException;
import com.vr.autorizador.exception.NonexistentCardException;
import com.vr.autorizador.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final String BODY = """
            {"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":10.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void shouldRespond201OkWhenTransactionIsAuthorized() throws Exception {
        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string("OK"));
    }

    @Test
    void shouldUseTheIdempotencyHeaderWhenProvided() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());

        verify(authorizationService).authorize(any(), eq("key-1"));
    }

    @Test
    void shouldPassANullKeyWhenTheIdempotencyHeaderIsAbsent() throws Exception {
        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        verify(authorizationService).authorize(any(), isNull());
    }

    @Test
    void shouldPassANullKeyWhenTheIdempotencyHeaderIsBlank() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());

        verify(authorizationService).authorize(any(), isNull());
    }

    @Test
    void shouldRespond201OkWhenTransactionWasAlreadyProcessed() throws Exception {
        doThrow(new AuthorizationService.TransactionAlreadyProcessedException("key-1", null))
                .when(authorizationService).authorize(any(), any());

        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string("OK"));
    }

    @Test
    void shouldRespond422WithInsufficientBalance() throws Exception {
        doThrow(new InsufficientBalanceException()).when(authorizationService).authorize(any(), any());

        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SALDO_INSUFICIENTE"));
    }

    @Test
    void shouldRespond422WithInvalidPassword() throws Exception {
        doThrow(new InvalidPasswordException()).when(authorizationService).authorize(any(), any());

        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SENHA_INVALIDA"));
    }

    @Test
    void shouldRespond422WithNonexistentCard() throws Exception {
        doThrow(new NonexistentCardException()).when(authorizationService).authorize(any(), any());

        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("CARTAO_INEXISTENTE"));
    }

    @Test
    void shouldRespond400WhenAmountIsInvalid() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"6549873025634501\",\"senhaCartao\":\"1234\",\"valor\":0}"))
                .andExpect(status().isBadRequest());
    }
}
