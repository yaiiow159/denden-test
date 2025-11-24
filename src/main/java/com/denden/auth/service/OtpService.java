package com.denden.auth.service;

/**
 * OTP (One-Time Password) 服務介面
 * 負責 OTP 的生成、驗證、儲存與失效管理
 * 
 * @author Timmy
 * @since 2.0.0
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
     */
    void createOtpSession(String email, String otp);
    
    /**
     * 根據 Email 驗證 OTP
     *      
     * @param email 使用者 Email
     * @param otp 使用者輸入的 OTP
     * @return 驗證是否成功
     */
    boolean verifyOtpByEmail(String email, String otp);
    
    /**
     * 檢查是否有活躍的 OTP 會話
     * 
     * @param email 使用者 Email
     * @return 是否有活躍會話
     */
    boolean hasActiveSession(String email);
    
    /**
     * 根據 Email 更新 OTP 會話（重新發送 OTP 時使用）
     *      
     * @param email 使用者 Email
     * @param newOtp 新的 OTP
     */
    void updateOtpSessionByEmail(String email, String newOtp);
}
