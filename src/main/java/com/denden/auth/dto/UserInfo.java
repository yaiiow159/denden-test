package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 使用者資訊 DTO
 * 
 * @param id 使用者 ID
 * @param email 使用者 Email
 * @param lastLoginAt 最後登入時間
 */
@Schema(description = "使用者資訊")
public record UserInfo(
    @Schema(description = "使用者 ID", example = "1")
    Long id,
    
    @Schema(description = "使用者 Email 地址", example = "user@example.com")
    String email,
    
    @Schema(description = "最後登入時間（ISO 8601 格式）", example = "2024-01-15T10:30:00")
    LocalDateTime lastLoginAt
) {}
