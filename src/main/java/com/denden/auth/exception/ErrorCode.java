package com.denden.auth.exception;

/**
 * 系統錯誤碼定義
 * 
 * <p>錯誤碼分類：
 * <ul>
 *   <li>1xxx - 認證錯誤</li>
 *   <li>2xxx - 註冊錯誤</li>
 *   <li>3xxx - Token 錯誤</li>
 *   <li>4xxx - 授權錯誤</li>
 *   <li>5xxx - 限流錯誤</li>
 *   <li>6xxx - 外部服務錯誤</li>
 *   <li>9xxx - 系統錯誤</li>
 * </ul>
 */
public enum ErrorCode {
    
    // ==================== 認證錯誤 (1xxx) ====================
    
    /**
     * 無效的登入憑證（Email 或密碼錯誤）
     */
    INVALID_CREDENTIALS(1001, "Email 或密碼錯誤"),
    
    /**
     * 帳號尚未啟用
     */
    ACCOUNT_NOT_ACTIVATED(1002, "帳號尚未啟用，請先驗證 Email"),
    
    /**
     * 帳號已被鎖定
     */
    ACCOUNT_LOCKED(1003, "帳號已被暫時鎖定，請稍後再試"),
    
    /**
     * 無效或過期的 OTP
     */
    INVALID_OTP(1004, "OTP 無效或已過期"),
    
    /**
     * OTP 驗證嘗試次數超過限制
     */
    OTP_ATTEMPTS_EXCEEDED(1005, "OTP 驗證失敗次數過多，請重新登入"),
    
    /**
     * OTP 會話不存在或已過期
     */
    OTP_SESSION_NOT_FOUND(1006, "OTP 會話不存在或已過期"),
    
    // ==================== 註冊錯誤 (2xxx) ====================
    
    /**
     * Email 已被註冊
     */
    EMAIL_ALREADY_EXISTS(2001, "此 Email 已被註冊"),
    
    /**
     * Email 格式不正確
     */
    INVALID_EMAIL_FORMAT(2002, "Email 格式不正確"),
    
    /**
     * 密碼強度不符合要求
     */
    WEAK_PASSWORD(2003, "密碼必須至少 8 字元，包含大小寫字母、數字和特殊字元"),
    
    /**
     * 使用者不存在
     */
    USER_NOT_FOUND(2004, "使用者不存在"),
    
    // ==================== Token 錯誤 (3xxx) ====================
    
    /**
     * 無效或過期的 Token
     */
    INVALID_TOKEN(3001, "Token 無效或已過期"),
    
    /**
     * Token 已被使用
     */
    TOKEN_ALREADY_USED(3002, "此驗證連結已被使用"),
    
    /**
     * Token 不存在
     */
    TOKEN_NOT_FOUND(3003, "驗證 Token 不存在"),
    
    /**
     * JWT Token 格式錯誤
     */
    INVALID_JWT_FORMAT(3004, "JWT Token 格式錯誤"),
    
    /**
     * JWT Token 簽章驗證失敗
     */
    JWT_SIGNATURE_INVALID(3005, "JWT Token 簽章驗證失敗"),
    
    // ==================== 授權錯誤 (4xxx) ====================
    
    /**
     * 未提供認證資訊
     */
    UNAUTHORIZED(4001, "請先登入"),
    
    /**
     * 權限不足
     */
    FORBIDDEN(4002, "權限不足，無法存取此資源"),
    
    /**
     * 嘗試存取其他使用者的資源
     */
    ACCESS_DENIED(4003, "無法存取其他使用者的資源"),
    
    // ==================== 限流錯誤 (5xxx) ====================
    
    /**
     * 請求頻率超過限制
     */
    RATE_LIMIT_EXCEEDED(5001, "請求過於頻繁，請稍後再試"),
    
    /**
     * 登入失敗次數過多
     */
    TOO_MANY_LOGIN_ATTEMPTS(5002, "登入失敗次數過多，帳號已被暫時鎖定"),
    
    // ==================== 外部服務錯誤 (6xxx) ====================
    
    /**
     * Email 發送失敗
     */
    EMAIL_SERVICE_ERROR(6001, "Email 發送失敗，請稍後再試"),
    
    /**
     * Email 服務暫時不可用
     */
    EMAIL_SERVICE_UNAVAILABLE(6002, "Email 服務暫時不可用"),
    
    /**
     * Redis 服務錯誤
     */
    REDIS_SERVICE_ERROR(6003, "快取服務錯誤"),
    
    /**
     * 資料庫服務錯誤
     */
    DATABASE_SERVICE_ERROR(6004, "資料庫服務錯誤"),
    
    // ==================== 系統錯誤 (9xxx) ====================
    
    /**
     * 內部伺服器錯誤
     */
    INTERNAL_ERROR(9001, "系統內部錯誤，請稍後再試"),
    
    /**
     * 參數驗證失敗
     */
    VALIDATION_ERROR(9002, "參數驗證失敗"),
    
    /**
     * 不支援的操作
     */
    UNSUPPORTED_OPERATION(9003, "不支援的操作"),
    
    /**
     * 資源不存在
     */
    RESOURCE_NOT_FOUND(9004, "請求的資源不存在");
    
    private final int code;
    private final String message;
    
    /**
     * 建構錯誤碼
     * 
     * @param code 錯誤碼數字
     * @param message 錯誤訊息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 取得錯誤碼
     * 
     * @return 錯誤碼數字
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 取得錯誤訊息
     * 
     * @return 錯誤訊息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 根據錯誤碼數字查找對應的 ErrorCode
     * 
     * @param code 錯誤碼數字
     * @return 對應的 ErrorCode，若不存在則返回 INTERNAL_ERROR
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
