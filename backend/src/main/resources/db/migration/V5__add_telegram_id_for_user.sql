ALTER TABLE _users
ADD COLUMN telegram_id BIGINT UNIQUE;

CREATE INDEX idx_users_telegram_id ON _users(telegram_id);