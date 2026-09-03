package com.vr.autorizador.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vr.autorizador.controller.dto.CardRequestDTO;
import com.vr.autorizador.controller.dto.CardResponseDTO;
import com.vr.autorizador.exception.CardAlreadyExistsException;
import com.vr.autorizador.exception.CardNotFoundException;
import com.vr.autorizador.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
class CardControllerTest {

    private static final String CARD_NUMBER = "6549873025634501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    void shouldRespond201WithTheCardNumberWhenCreatingCard() throws Exception {
        CardRequestDTO request = new CardRequestDTO(CARD_NUMBER, "1234");
        when(cardService.create(any())).thenReturn(CardResponseDTO.of(CARD_NUMBER));

        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCartao").value(CARD_NUMBER))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void shouldRespond422WithTheCardNumberWhenCardAlreadyExists() throws Exception {
        CardRequestDTO request = new CardRequestDTO(CARD_NUMBER, "1234");
        when(cardService.create(any())).thenThrow(new CardAlreadyExistsException(CARD_NUMBER));

        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.numeroCartao").value(CARD_NUMBER))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void shouldRespond400WhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"123\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRespond200WithTheCardBalance() throws Exception {
        when(cardService.getBalance(CARD_NUMBER)).thenReturn(new BigDecimal("495.15"));

        mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isOk())
                .andExpect(content().string("495.15"));
    }

    @Test
    void shouldRespond404WithoutBodyWhenCardDoesNotExist() throws Exception {
        when(cardService.getBalance(CARD_NUMBER)).thenThrow(new CardNotFoundException(CARD_NUMBER));

        String body = mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEmpty();
    }
}
