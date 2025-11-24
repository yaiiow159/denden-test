package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * OTP 驗證請求 DTO
 * 
 * <p>注意：使用 email
 * 
 * @param email 使用者 Email
 * @param otp 6 位數字 OTP 驗證碼
 */
@Schema(description = "OTP 驗證請求（第二階段 OTP 驗證）")
public record VerifyOtpRequest(
    @Schema(
        description = "使用者 Email（登入時使用的 Email）",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    String email,
    
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
