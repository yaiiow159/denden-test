package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 會員登入請求 DTO
 * 
 * @param email 使用者 Email，必須符合 Email 格式
 * @param password 使用者密碼
 */
@Schema(description = "會員登入請求（第一階段密碼驗證）")
public record LoginRequest(
    @Schema(
        description = "使用者 Email 地址",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    String email,
    
    @Schema(
        description = "使用者密碼",
        example = "SecurePass123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "密碼不能為空")
    String password
) {}
