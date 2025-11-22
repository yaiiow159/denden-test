package com.denden.auth.controller;

import com.denden.auth.dto.ErrorResponse;
import com.denden.auth.dto.UserInfo;
import com.denden.auth.service.UserService;
import com.denden.auth.util.MaskingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用者資訊查詢控制器
 * 
 * <p>提供已認證使用者查詢自己資訊的 API 端點：
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "使用者 API", description = "使用者資訊查詢相關 API（需要認證）")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 取得當前使用者資訊
     * 
     * <p>從 JWT Token 提取使用者身份，查詢並返回使用者的基本資訊</p>
     * 
     * <p><b>認證要求：</b> 需要有效的 JWT Token</p>
     * 
     * <p><b>權限控制：</b> 使用者只能查詢自己的資訊</p>
     * 
     * @return ResponseEntity 包含 UserInfo 的響應
     * 
     */
    @GetMapping("/me")
    @Operation(
        summary = "取得當前使用者資訊",
        description = "從 JWT Token 提取使用者身份，查詢並返回使用者的基本資訊（ID、Email、最後登入時間）"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "成功取得使用者資訊",
            content = @Content(schema = @Schema(implementation = UserInfo.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未提供 JWT Token 或 Token 無效",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "使用者不存在",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<UserInfo> getCurrentUser() {
        String email = getCurrentUserEmail();
        log.info("使用者資訊請求來自: {}", MaskingUtils.maskEmail(email));
        
        UserInfo userInfo = userService.getCurrentUserInfo(email);
        
        log.info("成功取得使用者資訊，使用者 ID: {}", userInfo.id());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 取得當前使用者最後登入時間
     * 
     * <p>從 JWT Token 提取使用者身份，查詢並返回最後登入時間</p>
     * 
     * <p><b>認證要求：</b> 需要有效的 JWT Token</p>
     * 
     * <p><b>權限控制：</b> 使用者只能查詢自己的登入時間</p>
     * 
     * @return ResponseEntity 包含最後登入時間的響應
     * 
     */
    @GetMapping("/me/last-login")
    @Operation(
        summary = "取得當前使用者最後登入時間",
        description = "從 JWT Token 提取使用者身份，查詢並返回最後登入時間。若使用者從未登入過，lastLoginAt 為 null"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "成功取得最後登入時間"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未提供 JWT Token 或 Token 無效",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "使用者不存在",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Map<String, LocalDateTime>> getLastLoginTime() {
        String email = getCurrentUserEmail();
        log.info("最後登入時間請求來自: {}", MaskingUtils.maskEmail(email));
        
        LocalDateTime lastLoginAt = userService.getLastLoginTime(email);
        
        Map<String, LocalDateTime> response = new HashMap<>();
        response.put("lastLoginAt", lastLoginAt);
        
        if (lastLoginAt != null) {
            log.info("成功取得最後登入時間: {}", lastLoginAt);
        } else {
            log.info("使用者從未登入過");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 從 SecurityContext 提取當前認證使用者的 Email
     *      * 
     * @return 當前使用者的 Email 地址
     * @throws IllegalStateException 當無法取得認證資訊時
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("SecurityContext 中找不到認證資訊");
            throw new IllegalStateException("使用者未認證");
        }
        
        String email = authentication.getName();
        log.debug("從 SecurityContext 提取 Email: {}", MaskingUtils.maskEmail(email));
        
        return email;
    }
}
