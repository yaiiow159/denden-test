package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * OTP 驗證請求 DTO
 * 
 * @param sessionId OTP 會話 ID
 * @param otp 6 位數字 OTP 驗證碼
 */
@Schema(description = "OTP 驗證請求（第二階段 OTP 驗證）")
public record VerifyOtpRequest(
    @Schema(
        description = "OTP 會話 ID（從登入 API 返回）",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Session ID 不能為空")
    String sessionId,
    
    @Schema(
        description = "6 位數字 OTP 驗證碼（從 Email 取得）",
        example = "123456",
        pattern = "\\d{6}",
        minLength = 6,
        maxLength = 6,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "OTP 不能為空")
    @Pattern(regexp = "\\d{6}", message = "OTP 必須為 6 位數字")
    String otp
) {}
