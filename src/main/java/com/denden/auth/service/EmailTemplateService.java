package com.denden.auth.service;

/**
 * 郵件模板服務介面
 * 
 * <p>負責構建各種類型的郵件 HTML 內容，將模板邏輯與郵件發送邏輯分離。</p>
 * 
 * @author Timmy
 * @since 1.0.0
 */
public interface EmailTemplateService {
    
    /**
     * 構建驗證郵件 HTML 內容
     * 
     * @param verificationLink 驗證連結
     * @return HTML 內容
     */
    String buildVerificationEmail(String verificationLink);
    
    /**
     * 構建 OTP 郵件 HTML 內容
     * 
     * @param otp OTP 驗證碼
     * @return HTML 內容
     */
    String buildOtpEmail(String otp);
    
    /**
     * 構建帳號鎖定通知郵件 HTML 內容
     * 
     * @return HTML 內容
     */
    String buildAccountLockedEmail();
    
    /**
     * 構建歡迎郵件 HTML 內容
     * 
     * @param username 使用者名稱
     * @return HTML 內容
     */
    String buildWelcomeEmail(String username);
}
