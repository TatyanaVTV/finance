-- V1__init_schema.sql
-- Начальная схема: users, categories, transactions, reports.

CREATE TABLE IF NOT EXISTS users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    registered_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
    id      UUID        PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    type    VARCHAR(20) NOT NULL,
    user_id UUID        NOT NULL,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_categories_name_per_user UNIQUE (name, user_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id          UUID           PRIMARY KEY,
    user_id     UUID           NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    category_id UUID,
    date        TIMESTAMP      NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT fk_transactions_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS reports (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    period       VARCHAR(20) NOT NULL,
    generated_at TIMESTAMP   NOT NULL,
    start_date   TIMESTAMP,
    end_date     TIMESTAMP,
    content      TEXT,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id)
);