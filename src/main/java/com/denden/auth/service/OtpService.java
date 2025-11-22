package com.denden.auth.service;

/**
 * OTP (One-Time Password) 服務介面
 * 負責 OTP 的生成、驗證、儲存與失效管理
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
public interface OtpService {
    
    /**
     * 產生 6 位數字 OTP
     * 
     * @return 6 位數字的 OTP 字串
     */
    String generateOtp();
    
    /**
     * 建立 OTP 會話並儲存至 Redis
     * 
     * @param email 使用者 Email
     * @param otp OTP 驗證碼
     * @return Session ID，用於後續驗證
     */
    String createOtpSession(String email, String otp);
    
    /**
     * 驗證 OTP 正確性
     * 
     * @param sessionId OTP 會話 ID
     * @param otp 使用者輸入的 OTP
     * @return true 表示驗證成功，false 表示驗證失敗
     */
    boolean validateOtp(String sessionId, String otp);
    
    /**
     * 記錄 OTP 錯誤嘗試次數
     * 
     * @param sessionId OTP 會話 ID
     * @return 當前錯誤嘗試次數
     */
    int incrementOtpAttempts(String sessionId);
    
    /**
     * 使 OTP 失效（刪除 Redis 中的會話）
     * 
     * @param sessionId OTP 會話 ID
     */
    void invalidateOtp(String sessionId);
    
    /**
     * 取得 OTP 會話中的 Email
     * 
     * @param sessionId OTP 會話 ID
     * @return Email 地址，若會話不存在則返回 null
     */
    String getEmailFromSession(String sessionId);
}
