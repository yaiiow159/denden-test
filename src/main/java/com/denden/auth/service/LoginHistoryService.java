package com.denden.auth.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登入歷史服務介面
 * 
 * <p>使用 Redis ZSet 記錄使用者登入時間，提供：
 * <ul>
 *   <li>記錄使用者登入時間</li>
 *   <li>查詢使用者最後登入時間</li>
 *   <li>查詢最近活躍的使用者</li>
 * </ul>
 * 
 * <p>Redis ZSet 結構：
 * <pre>
 * Key: login_history
 * Score: timestamp (毫秒)
 * Member: userId
 * </pre>
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
public interface LoginHistoryService {
    
    /**
     * 記錄使用者登入時間
     * 
     * @param userId 使用者 ID
     * @param loginTime 登入時間
     */
    void recordLoginTime(Long userId, LocalDateTime loginTime);
    
    /**
     * 取得使用者最後登入時間
     * 
     * @param userId 使用者 ID
     * @return 最後登入時間，若從未登入則返回 null
     */
    LocalDateTime getLastLoginTime(Long userId);
    
    /**
     * 取得最近活躍的使用者 ID 列表
     * 
     * @param limit 返回數量限制
     * @return 使用者 ID 列表，按登入時間降序排列
     */
    List<Long> getRecentActiveUsers(int limit);
    
    /**
     * 清理指定天數之前的登入記錄
     * 
     * @param daysAgo 保留最近幾天的記錄
     * @return 清理的記錄數量
     */
    long cleanOldLoginHistory(int daysAgo);
}
