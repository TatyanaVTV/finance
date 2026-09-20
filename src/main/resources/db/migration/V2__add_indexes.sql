-- V2__add_indexes.sql
-- Индексы для часто выполняемых запросов.

CREATE INDEX idx_categories_user_id ON categories (user_id);
CREATE INDEX idx_categories_user_type ON categories (user_id, type);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, date DESC);
CREATE INDEX idx_transactions_user_type_date ON transactions (user_id, type, date);
CREATE INDEX idx_transactions_category_id ON transactions (category_id);

CREATE INDEX idx_reports_user_id ON reports (user_id);