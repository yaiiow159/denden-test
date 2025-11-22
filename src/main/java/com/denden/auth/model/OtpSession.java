package com.denden.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * OTP Session 資料模型
 * 用於在 Redis 中儲存 OTP 驗證會話資訊
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 使用者 Email
     */
    private String email;
    
    /**
     * OTP 驗證碼
     */
    private String otp;
    
    /**
     * 錯誤嘗試次數
     */
    @Builder.Default
    private int attempts = 0;
    
    /**
     * 建立時間戳記（毫秒）
     */
    private long createdAt;
}
