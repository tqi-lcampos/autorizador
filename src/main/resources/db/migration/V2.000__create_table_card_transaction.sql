CREATE TABLE IF NOT EXISTS card_transaction
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    card_number     VARCHAR(16)    NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    CONSTRAINT card_transaction_pkey PRIMARY KEY (id),
    CONSTRAINT uk_card_transaction_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_card_transaction_card_number ON card_transaction (card_number);
