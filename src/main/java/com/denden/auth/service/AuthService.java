package com.denden.auth.service;

import com.denden.auth.dto.AuthResponse;
import com.denden.auth.dto.LoginRequest;
import com.denden.auth.dto.OtpResponse;
import com.denden.auth.dto.RegisterRequest;
import com.denden.auth.dto.VerifyOtpRequest;

/**
 * 認證服務介面
 * 
 * @author Timmy
 * @since 1.0.0
 */
public interface AuthService {
    
    /**
     * 註冊新會員
     * 
     * @param request 註冊請求，包含 Email 與密碼
     * @throws BusinessException 當 Email 已存在或密碼不符合要求時拋出
     */
    void register(RegisterRequest request);
    
    /**
     * 驗證 Email 並啟用帳號
     * 
     * @param token 驗證 Token
     * @throws BusinessException 當 Token 無效、已使用或過期時拋出
     */
    void verifyEmail(String token);
    
    /**
     * 重新發送驗證郵件
     * 
     * @param email 使用者 Email
     * @throws BusinessException 當帳號不存在或狀態不是 PENDING 時拋出
     */
    void resendVerificationEmail(String email);
    
    /**
     * 會員登入 - 第一階段密碼驗證
     * 
     * @param request 登入請求，包含 Email 與密碼
     * @param ipAddress 客戶端 IP 地址
     * @return OTP 響應，包含遮罩後的 Email 和有效期限
     * @throws BusinessException 當登入失敗時拋出
     */
    OtpResponse login(LoginRequest request, String ipAddress);
    
    /**
     * 會員登入 - 第二階段 OTP 驗證
     * 
     * 
     * @param request OTP 驗證請求，包含 email 與 OTP
     * @return 認證響應，包含 JWT Token 與使用者資訊
     * @throws BusinessException 當 OTP 驗證失敗時拋出
     */
    AuthResponse verifyOtp(VerifyOtpRequest request);
    
    /**
     * 重新發送 OTP
     *     
     * @param email 使用者 Email
     * @return OTP 響應，包含遮罩後的 Email 和有效期限
     * @throws BusinessException 當 email 無效或無活躍會話時拋出
     */
    OtpResponse resendOtp(String email);
}
