package com.denden.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 統一錯誤響應 DTO
 * 
 * @param code 錯誤碼
 * @param message 錯誤訊息
 * @param timestamp 錯誤發生時間
 * @param path 請求路徑
 */
@Schema(description = "錯誤響應")
public record ErrorResponse(
    @Schema(
      description = "錯誤碼（1xxx: 認證錯誤, 2xxx: 註冊錯誤, 3xxx: Token 錯誤, 4xxx: 授權錯誤, 5xxx: Rate Limiting, 6xxx: 外部服務錯誤, 9xxx: 系統錯誤）"
    , example = "1001")
    int code,
    
    @Schema(description = "錯誤訊息", example = "Invalid email or password")
    String message,
    
    @Schema(description = "錯誤發生時間（ISO 8601 格式）", example = "2024-01-15T10:30:00")
    LocalDateTime timestamp,
    
    @Schema(description = "請求路徑", example = "/api/v1/auth/login")
    String path
) {
    /**
     * 建立錯誤響應
     * 
     * @param code 錯誤碼
     * @param message 錯誤訊息
     * @param path 請求路徑
     * @return ErrorResponse 實例
     */
    public static ErrorResponse of(int code, String message, String path) {
        return new ErrorResponse(code, message, LocalDateTime.now(), path);
    }
}
