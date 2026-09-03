package com.vr.autorizador.service;

import com.vr.autorizador.controller.dto.TransactionRequestDTO;
import com.vr.autorizador.exception.InsufficientBalanceException;
import com.vr.autorizador.exception.InvalidPasswordException;
import com.vr.autorizador.exception.NonexistentCardException;
import com.vr.autorizador.model.Card;
import com.vr.autorizador.model.CardTransaction;
import com.vr.autorizador.repository.CardRepository;
import com.vr.autorizador.repository.CardTransactionRepository;
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
class AuthorizationServiceTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String KEY = "key-1";

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardTransactionRepository cardTransactionRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private Card cardWithBalance(String balance) {
        return Card.builder().cardNumber(CARD_NUMBER).password("1234").balance(new BigDecimal(balance)).build();
    }

    @Test
    void shouldDebitBalanceWhenTransactionIsAuthorized() {
        Card card = cardWithBalance("500.00");
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        authorizationService.authorize(new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), KEY);

        assertThat(card.getBalance()).isEqualByComparingTo("490.00");
        verify(cardRepository).save(card);

        ArgumentCaptor<CardTransaction> captor = ArgumentCaptor.forClass(CardTransaction.class);
        verify(cardTransactionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(KEY);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldAuthorizeTransactionWithAmountExactlyEqualToBalance() {
        Card card = cardWithBalance("10.00");
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        authorizationService.authorize(new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), KEY);

        assertThat(card.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldDeclineWhenCardDoesNotExist() {
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.authorize(
                new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), KEY))
                .isInstanceOf(NonexistentCardException.class);

        verify(cardTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldDeclineWhenPasswordIsInvalid() {
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(cardWithBalance("500.00")));

        assertThatThrownBy(() -> authorizationService.authorize(
                new TransactionRequestDTO(CARD_NUMBER, "9999", new BigDecimal("10.00")), KEY))
                .isInstanceOf(InvalidPasswordException.class);

        verify(cardTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldDeclineWhenBalanceIsInsufficient() {
        Card card = cardWithBalance("5.00");
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> authorizationService.authorize(
                new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), KEY))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(card.getBalance()).isEqualByComparingTo("5.00");
        verify(cardTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldDebitWithoutCheckingForResendsWhenThereIsNoIdempotencyKey() {
        Card card = cardWithBalance("500.00");
        when(cardRepository.findByCardNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        authorizationService.authorize(new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), null);

        assertThat(card.getBalance()).isEqualByComparingTo("490.00");
        verify(cardTransactionRepository, never()).existsByIdempotencyKey(any());

        ArgumentCaptor<CardTransaction> captor = ArgumentCaptor.forClass(CardTransaction.class);
        verify(cardTransactionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isNull();
    }

    @Test
    void shouldNotDebitAgainWhenIdempotencyKeyWasAlreadyUsed() {
        when(cardTransactionRepository.existsByIdempotencyKey(KEY)).thenReturn(true);

        authorizationService.authorize(new TransactionRequestDTO(CARD_NUMBER, "1234", new BigDecimal("10.00")), KEY);

        verify(cardRepository, never()).findByCardNumberForUpdate(any());
        verify(cardTransactionRepository, never()).saveAndFlush(any());
    }
}
