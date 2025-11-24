package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OTP 響應 DTO
 *  
 * @param message 提示訊息
 * @param expiresIn OTP 有效期限（秒）
 * @param maskedEmail 遮罩後的 Email 地址
 */
@Schema(description = "OTP 發送響應")
public record OtpResponse(
    @Schema(description = "提示訊息", example = "OTP 已發送至您的 Email，請查收")
    String message,
    
    @Schema(description = "OTP 有效期限（秒）", example = "300")
    Long expiresIn,
    
    @Schema(description = "遮罩後的 Email 地址", example = "u***@example.com")
    String maskedEmail
) {
    /**
     * 建立 OTP 響應
     * 
     * @param expiresIn 有效期限（秒）
     * @param maskedEmail 遮罩後的 Email
     * @return OTP 響應物件
     */
    public static OtpResponse of(Long expiresIn, String maskedEmail) {
        return new OtpResponse(
            "OTP 已發送至您的 Email，請查收",
            expiresIn,
            maskedEmail
        );
    }
}
