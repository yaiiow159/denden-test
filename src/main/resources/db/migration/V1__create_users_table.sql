CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS '會員使用者資料表';
COMMENT ON COLUMN users.id IS '使用者唯一識別碼';
COMMENT ON COLUMN users.email IS '使用者 Email（唯一）';
COMMENT ON COLUMN users.password_hash IS 'BCrypt 加密後的密碼';
COMMENT ON COLUMN users.status IS '帳號狀態：PENDING(待驗證), ACTIVE(已啟用), LOCKED(已鎖定)';
COMMENT ON COLUMN users.last_login_at IS '最後登入時間';
COMMENT ON COLUMN users.created_at IS '建立時間';
COMMENT ON COLUMN users.updated_at IS '更新時間';
