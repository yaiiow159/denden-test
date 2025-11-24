package com.denden.auth.scheduler;

import com.denden.auth.repository.LoginAttemptRepository;
import com.denden.auth.repository.VerificationTokenRepository;
import com.denden.auth.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 資料清理定時任務
 * 
 * <p>定期清理系統中的過期資料，使用批次處理</p>
 * 
 * @author Timmy
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupScheduler {
    
    private final LoginHistoryService loginHistoryService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final com.denden.auth.repository.OtpSessionRepository otpSessionRepository;
    
    @Value("${app.cleanup.login-history-retention-days:90}")
    private int loginHistoryRetentionDays;
    
    @Value("${app.cleanup.token-retention-days:30}")
    private int tokenRetentionDays;
    
    @Value("${app.cleanup.login-attempt-retention-days:30}")
    private int loginAttemptRetentionDays;
    
    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${app.cleanup.batch-size:1000}")
    private int batchSize;
    
    /**
     * 清理舊的登入記錄
     * 
     * <p>執行時間：每天凌晨 2 點
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
    
    /**
     * 批次清理過期的驗證 Token
     * 
     * <p>執行時間：每天凌晨 3 點
     */
    @Scheduled(cron = "${app.cleanup.token-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupExpiredTokens() {
        if (!cleanupEnabled) {
            log.debug("資料清理功能已停用");
            return;
        }
        
        log.info("開始執行過期 Token 清理任務");
        
        try {
            long startTime = System.currentTimeMillis();
            long totalDeleted = 0;
            int deletedCount;
            
            do {
                deletedCount = verificationTokenRepository.deleteExpiredTokensInBatch(
                    LocalDateTime.now(), batchSize
                );
                totalDeleted += deletedCount;
                
                if (deletedCount > 0) {
                    log.debug("批次刪除過期 Token: {} 筆", deletedCount);
                }
                
                if (deletedCount == batchSize) {
                    Thread.sleep(100);
                }
                
            } while (deletedCount > 0);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("過期 Token 清理完成，總刪除數量: {}, 耗時: {}ms", totalDeleted, duration);
            
        } catch (Exception e) {
            log.error("過期 Token 清理任務執行失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 批次清理已使用的舊 Token
     * 
     * <p>執行時間：每週日凌晨 4 點
     */
    @Scheduled(cron = "${app.cleanup.used-token-cron:0 0 4 * * SUN}")
    @Transactional
    public void cleanupUsedTokens() {
        if (!cleanupEnabled) {
            log.debug("資料清理功能已停用");
            return;
        }
        
        log.info("開始執行已使用 Token 清理任務，保留天數: {}", tokenRetentionDays);
        
        try {
            long startTime = System.currentTimeMillis();
            long totalDeleted = 0;
            int deletedCount;
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(tokenRetentionDays);
            
            do {
                deletedCount = verificationTokenRepository.deleteUsedTokensInBatch(
                    cutoffDate, batchSize
                );
                totalDeleted += deletedCount;
                
                if (deletedCount > 0) {
                    log.debug("批次刪除已使用 Token: {} 筆", deletedCount);
                }
                
                if (deletedCount == batchSize) {
                    Thread.sleep(100);
                }
                
            } while (deletedCount > 0);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("已使用 Token 清理完成，總刪除數量: {}, 耗時: {}ms", totalDeleted, duration);
            
        } catch (Exception e) {
            log.error("已使用 Token 清理任務執行失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 批次清理舊的登入嘗試記錄
     * 
     * <p>執行時間：每天凌晨 3:30
     */
    @Scheduled(cron = "${app.cleanup.login-attempt-cron:0 30 3 * * ?}")
    @Transactional
    public void cleanupOldLoginAttempts() {
        if (!cleanupEnabled) {
            log.debug("資料清理功能已停用");
            return;
        }
        
        log.info("開始執行登入嘗試記錄清理任務，保留天數: {}", loginAttemptRetentionDays);
        
        try {
            long startTime = System.currentTimeMillis();
            long totalDeleted = 0;
            int deletedCount;
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(loginAttemptRetentionDays);
            
            do {
                deletedCount = loginAttemptRepository.deleteOldAttemptsInBatch(
                    cutoffDate, batchSize
                );
                totalDeleted += deletedCount;
                
                if (deletedCount > 0) {
                    log.debug("批次刪除登入嘗試記錄: {} 筆", deletedCount);
                }
                
                if (deletedCount == batchSize) {
                    Thread.sleep(100);
                }
                
            } while (deletedCount > 0);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("登入嘗試記錄清理完成，總刪除數量: {}, 耗時: {}ms", totalDeleted, duration);
            
        } catch (Exception e) {
            log.error("登入嘗試記錄清理任務執行失敗: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理過期的 OTP Sessions（資料庫備援）
     * 
     * <p>執行時間：每小時執行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredOtpSessions() {
        if (!cleanupEnabled) {
            log.debug("資料清理功能已停用");
            return;
        }
        
        log.info("開始執行過期 OTP Sessions 清理任務");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = otpSessionRepository.deleteExpiredSessions(now);
            
            log.info("過期 OTP Sessions 清理完成 - 刪除記錄數: {}", deletedCount);
            
        } catch (Exception e) {
            log.error("OTP Sessions 清理任務執行失敗: {}", e.getMessage(), e);
        }
    }
}
