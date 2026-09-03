package com.vr.autorizador.service;

import com.vr.autorizador.controller.dto.CardRequestDTO;
import com.vr.autorizador.controller.dto.CardResponseDTO;
import com.vr.autorizador.exception.CardAlreadyExistsException;
import com.vr.autorizador.exception.CardNotFoundException;
import com.vr.autorizador.model.Card;
import com.vr.autorizador.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    @Transactional
    public CardResponseDTO create(CardRequestDTO request) {
        Optional.of(request.cardNumber())
                .filter(Predicate.not(cardRepository::existsByCardNumber))
                .orElseThrow(() -> new CardAlreadyExistsException(request.cardNumber()));

        Card card = Card.builder()
                .cardNumber(request.cardNumber())
                .password(request.password())
                .balance(Card.INITIAL_BALANCE)
                .build();

        try {
            cardRepository.saveAndFlush(card);
        } catch (DataIntegrityViolationException exception) {
            throw new CardAlreadyExistsException(request.cardNumber());
        }

        return CardResponseDTO.of(card.getCardNumber());
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .map(Card::getBalance)
                .orElseThrow(() -> new CardNotFoundException(cardNumber));
    }
}
