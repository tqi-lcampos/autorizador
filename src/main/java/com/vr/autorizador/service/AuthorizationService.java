package com.vr.autorizador.service;

import com.vr.autorizador.controller.dto.TransactionRequestDTO;
import com.vr.autorizador.exception.InsufficientBalanceException;
import com.vr.autorizador.exception.InvalidPasswordException;
import com.vr.autorizador.exception.NonexistentCardException;
import com.vr.autorizador.model.Card;
import com.vr.autorizador.model.CardTransaction;
import com.vr.autorizador.repository.CardRepository;
import com.vr.autorizador.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;

    /**
     * @param idempotencyKey chave enviada pelo cliente no header {@code Idempotency-Key}, ou
     *                       {@code null} quando o cliente nao envia. Sendo {@code null} nao ha
     *                       como reconhecer reenvios, e a transacao e sempre debitada.
     */
    @Transactional
    public void authorize(TransactionRequestDTO request, String idempotencyKey) {
        if (idempotencyKey == null) {
            log.warn("Transaction for card {} received without an idempotency key: a resend will be debited again",
                    request.cardNumber());
        } else if (cardTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Transaction {} already processed, nothing to debit", idempotencyKey);
            return;
        }

        Card card = cardRepository.findByCardNumberForUpdate(request.cardNumber())
                .orElseThrow(NonexistentCardException::new);

        if (!card.passwordMatches(request.cardPassword())) {
            throw new InvalidPasswordException();
        }
        if (!card.hasBalance(request.amount())) {
            throw new InsufficientBalanceException();
        }

        card.debit(request.amount());
        cardRepository.save(card);

        CardTransaction transaction = CardTransaction.builder()
                .cardNumber(request.cardNumber())
                .amount(request.amount())
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            cardTransactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            // Reenvio simultaneo da mesma transacao: a unique key impede o debito duplicado.
            throw new TransactionAlreadyProcessedException(idempotencyKey, exception);
        }

        publishEvent(transaction);
    }

    private void publishEvent(CardTransaction transaction) {
        log.debug("Transaction authorized for card {} with amount {}",
                transaction.getCardNumber(), transaction.getAmount());
    }

    public static class TransactionAlreadyProcessedException extends RuntimeException {

        public TransactionAlreadyProcessedException(String idempotencyKey, Throwable cause) {
            super("Transaction %s already processed.".formatted(idempotencyKey), cause);
        }
    }
}
