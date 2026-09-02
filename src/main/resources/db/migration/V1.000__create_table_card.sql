CREATE TABLE IF NOT EXISTS card
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    card_number VARCHAR(16)    NOT NULL,
    password    VARCHAR(100)   NOT NULL,
    balance     DECIMAL(15, 2) NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT card_pkey PRIMARY KEY (id),
    CONSTRAINT uk_card_card_number UNIQUE (card_number)
);
