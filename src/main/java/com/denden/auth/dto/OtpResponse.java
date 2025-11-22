package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OTP 響應 DTO
 * 
 * @param sessionId OTP 會話 ID，用於後續 OTP 驗證
 * @param message 提示訊息
 * @param expiresIn OTP 有效期限（秒）
 */
@Schema(description = "OTP 發送響應")
public record OtpResponse(
    @Schema(description = "OTP 會話 ID（用於 OTP 驗證）", example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId,
    
    @Schema(description = "提示訊息", example = "OTP 已發送至您的 Email，請查收")
    String message,
    
    @Schema(description = "OTP 有效期限（秒）", example = "300")
    Long expiresIn
) {
    /**
     * 建立 OTP 響應
     * 
     * @param sessionId OTP 會話 ID
     * @param expiresIn 有效期限（秒）
     * @return OTP 響應物件
     */
    public static OtpResponse of(String sessionId, Long expiresIn) {
        return new OtpResponse(
            sessionId,
            "OTP 已發送至您的 Email，請查收",
            expiresIn
        );
    }
}
