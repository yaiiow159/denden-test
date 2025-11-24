package com.denden.auth.entity;

/**
 * Token 類型列舉
 *
 * @author Timmy
 * @since 1.0.0
 */
public enum TokenType {
    /**
     * Email 驗證 Token
     * <p>
     * 用於新註冊帳號的 Email 地址驗證
     * 有效期限：24 小時
     * </p>
     */
    EMAIL_VERIFICATION,

    /**
     * 密碼重設 Token
     * <p>
     * 用於忘記密碼時的身份驗證
     * 有效期限：1 小時（建議）
     * 目前為預留功能
     * </p>
     */
    PASSWORD_RESET
}
