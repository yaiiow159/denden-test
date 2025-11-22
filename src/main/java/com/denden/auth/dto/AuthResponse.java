package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 認證成功響應 DTO
 * 
 * @param token JWT Token
 * @param tokenType Token 類型（通常為 "Bearer"）
 * @param expiresIn Token 有效期（秒）
 * @param user 使用者基本資訊
 */
@Schema(description = "認證成功響應")
public record AuthResponse(
    @Schema(description = "JWT Token（用於後續 API 認證）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,
    
    @Schema(description = "Token 類型", example = "Bearer")
    String tokenType,
    
    @Schema(description = "Token 有效期（秒）", example = "86400")
    Long expiresIn,
    
    @Schema(description = "使用者基本資訊")
    UserInfo user
) {
    /**
     * 建立 Bearer Token 響應
     * 
     * @param token JWT Token
     * @param expiresIn Token 有效期（秒）
     * @param user 使用者資訊
     * @return AuthResponse 實例
     */
    public static AuthResponse bearer(String token, Long expiresIn, UserInfo user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
