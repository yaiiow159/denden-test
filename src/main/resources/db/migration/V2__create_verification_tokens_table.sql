CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_token_type CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_tokens_token ON verification_tokens(token);
CREATE INDEX idx_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_tokens_expires_at ON verification_tokens(expires_at);
CREATE INDEX idx_tokens_type ON verification_tokens(type);
CREATE INDEX idx_tokens_used ON verification_tokens(used);

COMMENT ON TABLE verification_tokens IS 'Email 驗證與密碼重設 Token 資料表';
COMMENT ON COLUMN verification_tokens.id IS 'Token 唯一識別碼';
COMMENT ON COLUMN verification_tokens.token IS 'Token 字串（UUID）';
COMMENT ON COLUMN verification_tokens.user_id IS '關聯的使用者 ID';
COMMENT ON COLUMN verification_tokens.type IS 'Token 類型：EMAIL_VERIFICATION(Email驗證), PASSWORD_RESET(密碼重設)';
COMMENT ON COLUMN verification_tokens.expires_at IS 'Token 過期時間';
COMMENT ON COLUMN verification_tokens.used IS 'Token 是否已使用';
COMMENT ON COLUMN verification_tokens.created_at IS '建立時間';
