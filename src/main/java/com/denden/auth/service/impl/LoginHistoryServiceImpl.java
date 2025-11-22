package com.denden.auth.service.impl;

import com.denden.auth.service.LoginHistoryService;
import com.denden.auth.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登入歷史服務實作
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {
    
    private static final String LOGIN_HISTORY_KEY = "login_history";
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void recordLoginTime(Long userId, LocalDateTime loginTime) {
        try {
            double score = TimeUtils.toTimestampAsDouble(loginTime);
            redisTemplate.opsForZSet().add(LOGIN_HISTORY_KEY, userId.toString(), score);
            
            log.debug("記錄使用者登入時間到 Redis ZSet，User ID: {}, 時間: {}, Score: {}", 
                    userId, loginTime, score);
        } catch (Exception e) {
            log.error("記錄登入時間到 Redis 失敗，User ID: {}, 錯誤: {}", 
                    userId, e.getMessage(), e);
        }
    }
    
    @Override
    public LocalDateTime getLastLoginTime(Long userId) {
        try {
            Double score = redisTemplate.opsForZSet().score(LOGIN_HISTORY_KEY, userId.toString());
            
            if (score == null) {
                log.debug("使用者登入記錄不存在，User ID: {}", userId);
                return null;
            }
            
            LocalDateTime loginTime = TimeUtils.fromTimestamp(score);
            log.debug("從 Redis ZSet 取得使用者最後登入時間，User ID: {}, 時間: {}", 
                    userId, loginTime);
            
            return loginTime;
        } catch (Exception e) {
            log.error("從 Redis 取得登入時間失敗，User ID: {}, 錯誤: {}", 
                    userId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public List<Long> getRecentActiveUsers(int limit) {
        try {
            Set<String> userIds = redisTemplate.opsForZSet()
                    .reverseRange(LOGIN_HISTORY_KEY, 0, limit - 1);
            
            if (userIds == null || userIds.isEmpty()) {
                log.debug("沒有找到活躍使用者記錄");
                return List.of();
            }
            
            List<Long> result = userIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            
            log.debug("取得最近活躍使用者，數量: {}", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("取得最近活躍使用者失敗，錯誤: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public long cleanOldLoginHistory(int daysAgo) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysAgo);
            double maxScore = TimeUtils.toTimestampAsDouble(cutoffTime);
            
            Long removed = redisTemplate.opsForZSet()
                    .removeRangeByScore(LOGIN_HISTORY_KEY, 0, maxScore);
            
            if (removed != null && removed > 0) {
                log.info("清理舊的登入記錄，刪除數量: {}, 截止時間: {}", removed, cutoffTime);
            }
            
            return removed != null ? removed : 0;
            
        } catch (Exception e) {
            log.error("清理舊登入記錄失敗，錯誤: {}", e.getMessage(), e);
            return 0;
        }
    }
}
