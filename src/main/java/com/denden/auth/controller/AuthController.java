package com.denden.auth.controller;

import com.denden.auth.dto.AuthResponse;
import com.denden.auth.dto.ErrorResponse;
import com.denden.auth.dto.LoginRequest;
import com.denden.auth.dto.OtpResponse;
import com.denden.auth.dto.RegisterRequest;
import com.denden.auth.dto.ResendVerificationRequest;
import com.denden.auth.dto.VerifyOtpRequest;
import com.denden.auth.service.AuthService;
import com.denden.auth.util.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認證控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "認證 API", description = "會員註冊、登入與驗證相關 API")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @Operation(
        summary = "會員註冊",
        description = "使用 Email 與密碼註冊新帳號，系統會發送驗證郵件至指定 Email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "註冊成功，驗證郵件已發送"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數錯誤（Email 格式不正確或密碼不符合要求）",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email 已被註冊",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "請求過於頻繁",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到會員註冊請求");
        
        authService.register(request);
        
        log.info("會員註冊請求處理完成");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("/verify-email")
    @Operation(
        summary = "驗證 Email 並啟用帳號",
        description = "使用者點擊 Email 中的驗證連結後，系統驗證 Token 並啟用帳號"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email 驗證成功，帳號已啟用"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token 無效或已過期",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Token 不存在",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Token 已被使用",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        log.info("收到 Email 驗證請求");
        
        authService.verifyEmail(token);
        
        log.info("Email 驗證請求處理完成");
        return ResponseEntity.ok("Email 驗證成功，帳號已啟用");
    }
    
    @PostMapping("/resend-verification")
    @Operation(
        summary = "重新發送驗證郵件",
        description = "為尚未驗證的帳號重新發送驗證郵件"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "驗證郵件已重新發送"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數錯誤或帳號狀態不正確",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "使用者不存在",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "請求過於頻繁",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        log.info("收到重新發送驗證郵件請求");
        
        authService.resendVerificationEmail(request.email());
        
        log.info("重新發送驗證郵件請求處理完成");
        return ResponseEntity.ok("驗證郵件已重新發送");
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "會員登入 - 第一階段密碼驗證",
        description = "驗證 Email 與密碼，成功後發送 OTP 至使用者 Email，返回 sessionId 用於第二階段驗證"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "密碼驗證成功，OTP 已發送",
            content = @Content(schema = @Schema(implementation = OtpResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Email 或密碼錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "帳號尚未啟用或已被鎖定",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "請求過於頻繁或登入失敗次數過多",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<OtpResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("收到會員登入請求");
        
        String ipAddress = RequestUtils.getClientIp(httpRequest);
        OtpResponse response = authService.login(request, ipAddress);
        
        log.info("會員登入請求處理完成");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify-otp")
    @Operation(
        summary = "會員登入 - 第二階段 OTP 驗證",
        description = "驗證使用者輸入的 OTP，驗證成功後返回 JWT Token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP 驗證成功，返回 JWT Token",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數錯誤或 OTP 格式不正確",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "OTP 無效或已過期",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "OTP 驗證失敗次數過多",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OTP 會話不存在或已過期",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "請求過於頻繁",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("收到 OTP 驗證請求");
        
        AuthResponse response = authService.verifyOtp(request);
        
        log.info("OTP 驗證請求處理完成");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-otp")
    @Operation(
        summary = "重新發送 OTP",
        description = "為有效的 OTP 會話重新發送新的 OTP 至使用者 Email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP 已重新發送",
            content = @Content(schema = @Schema(implementation = OtpResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "請求參數錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OTP 會話不存在或已過期",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "請求過於頻繁",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系統內部錯誤",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<OtpResponse> resendOtp(@RequestParam("sessionId") String sessionId) {
        log.info("收到重新發送 OTP 請求");
        
        OtpResponse response = authService.resendOtp(sessionId);
        
        log.info("重新發送 OTP 請求處理完成");
        return ResponseEntity.ok(response);
    }
    
}
