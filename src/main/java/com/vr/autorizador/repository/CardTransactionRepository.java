package com.vr.autorizador.repository;

import com.vr.autorizador.model.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
