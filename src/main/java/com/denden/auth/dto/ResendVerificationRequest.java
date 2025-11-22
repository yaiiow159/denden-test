package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 重新發送驗證郵件請求 DTO
 * 
 * @param email 使用者 Email 地址
 */
@Schema(description = "重新發送驗證郵件請求")
public record ResendVerificationRequest(
    
    @Schema(description = "使用者 Email 地址", example = "user@example.com")
    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    String email
) {}
