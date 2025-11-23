package com.denden.auth.scheduler;

import com.denden.auth.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 資料清理定時任務
 * 
 * <p>定期清理系統中的過期資料</p>
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupScheduler {
    
    private final LoginHistoryService loginHistoryService;
    
    @Value("${app.cleanup.login-history-retention-days:90}")
    private int loginHistoryRetentionDays;
    
    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    /**
     * 清理舊的登入記錄
     * 
     * <p>執行時間：每天凌晨 2 點
     * <p>保留天數：預設 90 天（可配置）
     * 
     */
    @Scheduled(cron = "${app.cleanup.login-history-cron:0 0 2 * * ?}")
    public void cleanupOldLoginHistory() {
        if (!cleanupEnabled) {
            log.debug("資料清理功能已停用");
            return;
        }
        
        log.info("開始執行登入記錄清理任務，保留天數: {}", loginHistoryRetentionDays);
        
        try {
            long startTime = System.currentTimeMillis();
            long removedCount = loginHistoryService.cleanOldLoginHistory(loginHistoryRetentionDays);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("登入記錄清理完成，刪除數量: {}, 耗時: {}ms", removedCount, duration);
            
            if (removedCount > 0) {
                log.info("清理統計 - 截止時間: {}, 刪除記錄數: {}", 
                    LocalDateTime.now().minusDays(loginHistoryRetentionDays), 
                    removedCount);
            }
            
        } catch (Exception e) {
            log.error("登入記錄清理任務執行失敗: {}", e.getMessage(), e);
        }
    }
    
}
