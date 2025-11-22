package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 會員註冊請求 DTO
 * 
 * @param email 使用者 Email，必須符合 Email 格式
 * @param password 使用者密碼，長度 8-100 字元
 */
@Schema(description = "會員註冊請求")
public record RegisterRequest(
    @Schema(
        description = "使用者 Email 地址",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    String email,
    
    @Schema(
        description = "使用者密碼（至少 8 字元，需包含大小寫字母、數字和特殊字元）",
        example = "SecurePass123!",
        minLength = 8,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 100, message = "密碼長度必須在 8-100 字元之間")
    String password
) {}
