package com.vr.autorizador;

import com.vr.autorizador.repository.CardRepository;
import com.vr.autorizador.repository.CardTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AutorizadorFlowIntegrationTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD = "1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardTransactionRepository cardTransactionRepository;

    @BeforeEach
    void clean() {
        cardTransactionRepository.deleteAll();
        cardRepository.deleteAll();
    }

    private void createCard() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"%s\",\"senha\":\"%s\"}".formatted(CARD_NUMBER, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private void expectBalance(String balance) throws Exception {
        mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isOk())
                .andExpect(content().string(balance));
    }

    private org.springframework.test.web.servlet.ResultActions transaction(String cardNumber, String password,
                                                                           String amount, String key) throws Exception {
        var request = post("/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"%s\",\"senhaCartao\":\"%s\",\"valor\":%s}"
                        .formatted(cardNumber, password, amount));
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    @Test
    void shouldCreateCardStartingWithBalanceOfFiveHundred() throws Exception {
        createCard();
        expectBalance("500.00");
    }

    @Test
    void shouldRespond422WithTheCardNumberWhenCardAlreadyExists() throws Exception {
        createCard();

        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"%s\",\"senha\":\"%s\"}".formatted(CARD_NUMBER, PASSWORD)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"numeroCartao\":\"%s\"}".formatted(CARD_NUMBER), true));
    }

    @Test
    void shouldDebitUntilBalanceIsExhaustedAndThenDecline() throws Exception {
        createCard();

        String[] expectedBalances = {"400.00", "300.00", "200.00", "100.00", "0.00"};
        for (String expectedBalance : expectedBalances) {
            transaction(CARD_NUMBER, PASSWORD, "100.00", null)
                    .andExpect(status().isCreated())
                    .andExpect(content().string("OK"));
            expectBalance(expectedBalance);
        }

        transaction(CARD_NUMBER, PASSWORD, "0.01", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SALDO_INSUFICIENTE"));
        expectBalance("0.00");
    }

    @Test
    void shouldDeclineTransactionWithInvalidPassword() throws Exception {
        createCard();

        transaction(CARD_NUMBER, "9999", "10.00", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SENHA_INVALIDA"));
        expectBalance("500.00");
    }

    @Test
    void shouldDeclineTransactionOfNonexistentCard() throws Exception {
        transaction("1111222233334444", PASSWORD, "10.00", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("CARTAO_INEXISTENTE"));
    }

    @Test
    void shouldRespond404WithoutBodyForBalanceOfNonexistentCard() throws Exception {
        mockMvc.perform(get("/cartoes/{numeroCartao}", "1111222233334444"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void shouldNotDebitTwiceWithTheSameIdempotencyKey() throws Exception {
        createCard();

        transaction(CARD_NUMBER, PASSWORD, "10.00", "repeated-key").andExpect(status().isCreated());
        transaction(CARD_NUMBER, PASSWORD, "10.00", "repeated-key").andExpect(status().isCreated());

        expectBalance("490.00");
    }
}
