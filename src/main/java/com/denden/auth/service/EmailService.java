package com.denden.auth.service;

/**
 * Email 發送服務介面
 * 
 * @author Timmy
 * @since 1.0.0
 */
public interface EmailService {
    
    /**
     * 發送帳號驗證郵件
     * 
     * <p>發送包含驗證連結的郵件給新註冊使用者，用於驗證 Email 地址的有效性
     * 
     * @param to 收件者 Email 地址
     * @param token 驗證 Token，用於生成驗證連結
     * @throws BusinessException 當郵件發送失敗時拋出
     */
    void sendVerificationEmail(String to, String token);
    
    /**
     * 發送 OTP 驗證碼郵件
     * 
     * <p>發送包含 6 位數字 OTP 的郵件，用於雙因素認證的第二階段驗證
     * 
     * @param to 收件者 Email 地址
     * @param otp 6 位數字的一次性密碼
     * @throws BusinessException 當郵件發送失敗時拋出
     */
    void sendOtpEmail(String to, String otp);
    
    /**
     * 發送帳號鎖定通知郵件
     * 
     * <p>當使用者帳號因連續登入失敗而被鎖定時，發送通知郵件告知使用者
     * 
     * @param to 收件者 Email 地址
     * @throws BusinessException 當郵件發送失敗時拋出
     */
    void sendAccountLockedEmail(String to);
    
    /**
     * 發送歡迎郵件
     * 
     * <p>當使用者完成 Email 驗證後，發送歡迎郵件
     * 
     * @param to 收件者 Email 地址
     * @param username 使用者名稱
     * @throws BusinessException 當郵件發送失敗時拋出
     */
    void sendWelcomeEmail(String to, String username);
}
