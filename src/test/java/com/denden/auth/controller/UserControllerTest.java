package com.denden.auth.controller;

import com.denden.auth.dto.UserInfo;
import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 單元測試
 * 
 * <p>測試使用者控制器的所有端點，包括：
 * <ul>
 *   <li>取得當前使用者資訊</li>
 *   <li>取得最後登入時間</li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 單元測試")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("取得當前使用者資訊成功 - 應返回 200 OK")
    @WithMockUser(username = "test@example.com")
    void testGetCurrentUserSuccess() throws Exception {
        // Given
        LocalDateTime lastLoginAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        UserInfo userInfo = new UserInfo(1L, "test@example.com", lastLoginAt);
        
        when(userService.getCurrentUserInfo(anyString())).thenReturn(userInfo);
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.lastLoginAt").exists());
        
        verify(userService, times(1)).getCurrentUserInfo("test@example.com");
    }

    @Test
    @DisplayName("取得當前使用者資訊失敗 - 使用者不存在")
    @WithMockUser(username = "notfound@example.com")
    void testGetCurrentUserNotFound() throws Exception {
        // Given
        when(userService.getCurrentUserInfo(anyString()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        
        verify(userService, times(1)).getCurrentUserInfo("notfound@example.com");
    }

    @Test
    @DisplayName("取得最後登入時間成功 - 應返回 200 OK")
    @WithMockUser(username = "test@example.com")
    void testGetLastLoginTimeSuccess() throws Exception {
        // Given
        LocalDateTime lastLoginAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        
        when(userService.getLastLoginTime(anyString())).thenReturn(lastLoginAt);
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/me/last-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLoginAt").exists());
        
        verify(userService, times(1)).getLastLoginTime("test@example.com");
    }

    @Test
    @DisplayName("取得最後登入時間 - 從未登入過")
    @WithMockUser(username = "test@example.com")
    void testGetLastLoginTimeNeverLoggedIn() throws Exception {
        // Given
        when(userService.getLastLoginTime(anyString())).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/me/last-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLoginAt").isEmpty());
        
        verify(userService, times(1)).getLastLoginTime("test@example.com");
    }

    @Test
    @DisplayName("取得最後登入時間失敗 - 使用者不存在")
    @WithMockUser(username = "notfound@example.com")
    void testGetLastLoginTimeUserNotFound() throws Exception {
        // Given
        when(userService.getLastLoginTime(anyString()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/me/last-login"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        
        verify(userService, times(1)).getLastLoginTime("notfound@example.com");
    }
}
