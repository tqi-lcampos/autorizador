package com.vr.autorizador.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "card_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Nula quando o cliente nao envia o header {@code Idempotency-Key}. A unique constraint
     * ignora nulos, entao transacoes sem chave nunca colidem entre si.
     */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
