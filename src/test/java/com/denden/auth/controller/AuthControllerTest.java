package com.denden.auth.controller;

import com.denden.auth.dto.AuthResponse;
import com.denden.auth.dto.LoginRequest;
import com.denden.auth.dto.OtpResponse;
import com.denden.auth.dto.RegisterRequest;
import com.denden.auth.dto.ResendVerificationRequest;
import com.denden.auth.dto.UserInfo;
import com.denden.auth.dto.VerifyOtpRequest;
import com.denden.auth.exception.AuthenticationException;
import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.exception.ResourceNotFoundException;
import com.denden.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 單元測試
 * 
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.denden.auth.config.TestSecurityConfig.class)
@DisplayName("AuthController 單元測試")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("註冊成功 - 應返回 201 Created")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "SecurePass123!");
        
        doNothing().when(authService).register(any(RegisterRequest.class));
        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("註冊失敗 - Email 已被註冊")
    void testRegisterEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@example.com", "SecurePass123!");
        
        doThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS))
                .when(authService).register(any(RegisterRequest.class));
        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
        
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("註冊失敗 - 密碼強度不足")
    void testRegisterWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "weak");
        
        // 密碼驗證在 Bean Validation 層面就會失敗，返回 VALIDATION_ERROR (9002)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
        
        // 因為在 Bean Validation 層面就失敗了，所以不會調用 authService
        verify(authService, times(0)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Email 驗證成功 - 應返回 200 OK")
    void testVerifyEmailSuccess() throws Exception {
        String token = "valid-token-123";
        
        doNothing().when(authService).verifyEmail(token);
        
        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Email 驗證成功，帳號已啟用"));
        
        verify(authService, times(1)).verifyEmail(token);
    }

    @Test
    @DisplayName("Email 驗證失敗 - Token 無效")
    void testVerifyEmailInvalidToken() throws Exception {
        String token = "invalid-token";
        
        doThrow(new BusinessException(ErrorCode.INVALID_TOKEN))
                .when(authService).verifyEmail(token);
        
        // INVALID_TOKEN (3001) 屬於 3000-3999 範圍，映射到 UNAUTHORIZED (401)
        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()));
        
        verify(authService, times(1)).verifyEmail(token);
    }

    @Test
    @DisplayName("重新發送驗證郵件成功 - 應返回 200 OK")
    void testResendVerificationSuccess() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        
        doNothing().when(authService).resendVerificationEmail(anyString());
        
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("驗證郵件已重新發送"));
        
        verify(authService, times(1)).resendVerificationEmail(request.email());
    }

    @Test
    @DisplayName("重新發送驗證郵件失敗 - 使用者不存在")
    void testResendVerificationUserNotFound() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("notfound@example.com");
        
        doThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                .when(authService).resendVerificationEmail(anyString());
        
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        
        verify(authService, times(1)).resendVerificationEmail(request.email());
    }

    @Test
    @DisplayName("登入成功 - 應返回 OTP Session ID")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "SecurePass123!");
        OtpResponse otpResponse = OtpResponse.of("session-123", 300L);
        
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(otpResponse);
        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.expiresIn").value(300));
        
        verify(authService, times(1)).login(any(LoginRequest.class), anyString());
    }

    @Test
    @DisplayName("登入失敗 - 密碼錯誤")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "WrongPass123!");
        
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
        
        verify(authService, times(1)).login(any(LoginRequest.class), anyString());
    }

    @Test
    @DisplayName("登入失敗 - 帳號未啟用")
    void testLoginAccountNotActivated() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "SecurePass123!");
        
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED));
        
        // ACCOUNT_NOT_ACTIVATED (1002) 屬於 1000-1999 範圍，映射到 UNAUTHORIZED (401)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_NOT_ACTIVATED.getCode()));
        
        verify(authService, times(1)).login(any(LoginRequest.class), anyString());
    }

    @Test
    @DisplayName("登入失敗 - 帳號已鎖定")
    void testLoginAccountLocked() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "SecurePass123!");
        
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED));
        
        // ACCOUNT_LOCKED (1003) 屬於 1000-1999 範圍，映射到 UNAUTHORIZED (401)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_LOCKED.getCode()));
        
        verify(authService, times(1)).login(any(LoginRequest.class), anyString());
    }

    @Test
    @DisplayName("OTP 驗證成功 - 應返回 JWT Token")
    void testVerifyOtpSuccess() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("session-123", "123456");
        UserInfo userInfo = new UserInfo(1L, "test@example.com", LocalDateTime.now());
        AuthResponse authResponse = AuthResponse.bearer("jwt-token-123", 86400L, userInfo);
        
        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(authResponse);
        
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
        
        verify(authService, times(1)).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("OTP 驗證失敗 - OTP 無效")
    void testVerifyOtpInvalidOtp() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("session-123", "wrong-otp");
        
        // OTP 格式驗證在 Bean Validation 層面就會失敗，返回 VALIDATION_ERROR (9002)
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
        
        // 因為在 Bean Validation 層面就失敗了，所以不會調用 authService
        verify(authService, times(0)).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("OTP 驗證失敗 - Session 不存在")
    void testVerifyOtpSessionNotFound() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("invalid-session", "123456");
        
        when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.OTP_SESSION_NOT_FOUND));
        
        // OTP_SESSION_NOT_FOUND (1006) 屬於 1000-1999 範圍，映射到 UNAUTHORIZED (401)
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.OTP_SESSION_NOT_FOUND.getCode()));
        
        verify(authService, times(1)).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("重新發送 OTP 成功 - 應返回新的 Session ID")
    void testResendOtpSuccess() throws Exception {
        String sessionId = "session-123";
        OtpResponse otpResponse = OtpResponse.of("new-session-456", 300L);
        
        when(authService.resendOtp(sessionId)).thenReturn(otpResponse);
        
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("new-session-456"))
                .andExpect(jsonPath("$.expiresIn").value(300));
        
        verify(authService, times(1)).resendOtp(sessionId);
    }

    @Test
    @DisplayName("重新發送 OTP 失敗 - Session 不存在")
    void testResendOtpSessionNotFound() throws Exception {
        String sessionId = "invalid-session";
        
        when(authService.resendOtp(sessionId))
                .thenThrow(new BusinessException(ErrorCode.OTP_SESSION_NOT_FOUND));
        
        // OTP_SESSION_NOT_FOUND (1006) 屬於 1000-1999 範圍，映射到 UNAUTHORIZED (401)
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .param("sessionId", sessionId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.OTP_SESSION_NOT_FOUND.getCode()));
        
        verify(authService, times(1)).resendOtp(sessionId);
    }
}