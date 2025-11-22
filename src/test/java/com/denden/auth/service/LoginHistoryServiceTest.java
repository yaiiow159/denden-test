package com.denden.auth.service;

import com.denden.auth.service.impl.LoginHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginHistoryService 測試
 */
@ExtendWith(MockitoExtension.class)
class LoginHistoryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private LoginHistoryServiceImpl loginHistoryService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void recordLoginTime_應該成功記錄登入時間() {
        // Given
        Long userId = 1L;
        LocalDateTime loginTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        double expectedScore = loginTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // When
        loginHistoryService.recordLoginTime(userId, loginTime);

        // Then
        verify(zSetOperations).add("login_history", "1", expectedScore);
    }

    @Test
    void getLastLoginTime_應該返回登入時間() {
        // Given
        Long userId = 1L;
        LocalDateTime expectedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        double score = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        when(zSetOperations.score("login_history", "1")).thenReturn(score);

        // When
        LocalDateTime result = loginHistoryService.getLastLoginTime(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void getLastLoginTime_當無記錄時應該返回null() {
        // Given
        Long userId = 1L;
        when(zSetOperations.score("login_history", "1")).thenReturn(null);

        // When
        LocalDateTime result = loginHistoryService.getLastLoginTime(userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getRecentActiveUsers_應該返回最近活躍使用者列表() {
        // Given
        Set<String> userIds = Set.of("1", "2", "3");
        when(zSetOperations.reverseRange("login_history", 0, 9)).thenReturn(userIds);

        // When
        List<Long> result = loginHistoryService.getRecentActiveUsers(10);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void cleanOldLoginHistory_應該清理舊記錄() {
        // Given
        int daysAgo = 30;
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(5L);

        // When
        long removed = loginHistoryService.cleanOldLoginHistory(daysAgo);

        // Then
        assertThat(removed).isEqualTo(5L);
        verify(zSetOperations).removeRangeByScore(eq("login_history"), eq(0.0), anyDouble());
    }
}
