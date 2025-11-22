CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    successful BOOLEAN NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempts_email ON login_attempts(email);
CREATE INDEX idx_attempts_attempted_at ON login_attempts(attempted_at);
CREATE INDEX idx_attempts_email_attempted_at ON login_attempts(email, attempted_at);
CREATE INDEX idx_attempts_successful ON login_attempts(successful);

COMMENT ON TABLE login_attempts IS '登入嘗試記錄資料表';
COMMENT ON COLUMN login_attempts.id IS '記錄唯一識別碼';
COMMENT ON COLUMN login_attempts.email IS '嘗試登入的 Email';
COMMENT ON COLUMN login_attempts.ip_address IS '來源 IP 位址';
COMMENT ON COLUMN login_attempts.successful IS '登入是否成功';
COMMENT ON COLUMN login_attempts.attempted_at IS '嘗試時間';
