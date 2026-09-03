package com.vr.autorizador.service;

import com.vr.autorizador.controller.dto.CardRequestDTO;
import com.vr.autorizador.controller.dto.CardResponseDTO;
import com.vr.autorizador.exception.CardAlreadyExistsException;
import com.vr.autorizador.exception.CardNotFoundException;
import com.vr.autorizador.model.Card;
import com.vr.autorizador.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final String CARD_NUMBER = "6549873025634501";

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void shouldCreateCardWithInitialBalanceOfFiveHundred() {
        CardRequestDTO request = new CardRequestDTO(CARD_NUMBER, "1234");
        when(cardRepository.existsByCardNumber(CARD_NUMBER)).thenReturn(false);

        CardResponseDTO response = cardService.create(request);

        assertThat(response.cardNumber()).isEqualTo(CARD_NUMBER);

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCardNumber()).isEqualTo(CARD_NUMBER);
        assertThat(captor.getValue().getPassword()).isEqualTo("1234");
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldFailWhenCardAlreadyExists() {
        CardRequestDTO request = new CardRequestDTO(CARD_NUMBER, "1234");
        when(cardRepository.existsByCardNumber(CARD_NUMBER)).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(CardAlreadyExistsException.class)
                .extracting(exception -> ((CardAlreadyExistsException) exception).getCard())
                .isEqualTo(CardResponseDTO.of(CARD_NUMBER));

        verify(cardRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReturnTheCardBalance() {
        when(cardRepository.findByCardNumber(CARD_NUMBER))
                .thenReturn(Optional.of(Card.builder().balance(new BigDecimal("495.15")).build()));

        assertThat(cardService.getBalance(CARD_NUMBER)).isEqualByComparingTo("495.15");
    }

    @Test
    void shouldFailWhenQueryingBalanceOfNonexistentCard() {
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getBalance(CARD_NUMBER))
                .isInstanceOf(CardNotFoundException.class);
    }
}
