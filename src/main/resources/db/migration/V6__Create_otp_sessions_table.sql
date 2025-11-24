CREATE TABLE IF NOT EXISTS otp_sessions (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_otp_email ON otp_sessions(email);
CREATE INDEX idx_otp_expires_at ON otp_sessions(expires_at);

COMMENT ON TABLE otp_sessions IS 'OTP 會話表（Redis 備援）';
COMMENT ON COLUMN otp_sessions.id IS '主鍵';
COMMENT ON COLUMN otp_sessions.email IS '使用者 Email';
COMMENT ON COLUMN otp_sessions.otp IS '6 位數字 OTP';
COMMENT ON COLUMN otp_sessions.attempts IS '驗證嘗試次數';
COMMENT ON COLUMN otp_sessions.created_at IS '創建時間';
COMMENT ON COLUMN otp_sessions.expires_at IS '過期時間';
COMMENT ON COLUMN otp_sessions.used IS '是否已使用';
