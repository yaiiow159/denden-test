package com.denden.auth.service.impl;

import com.denden.auth.dto.UserInfo;
import com.denden.auth.entity.User;
import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.repository.UserRepository;
import com.denden.auth.service.LoginHistoryService;
import com.denden.auth.service.UserService;
import com.denden.auth.util.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 使用者服務實作
 * 
 * @author Timmy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LoginHistoryService loginHistoryService;

    /**
     * 取得當前使用者資訊
     * 
     * <p>優先從 Redis ZSet 讀取最後登入時間，若 Redis 無資料則從資料庫讀取
     * 
     * @param email 使用者 Email 地址
     * @return UserInfo 使用者資訊 DTO
     * @throws BusinessException 當使用者不存在時拋出 USER_NOT_FOUND
     */
    @Override
    @Transactional(readOnly = true)
    public UserInfo getCurrentUserInfo(String email) {
        log.debug("取得使用者資訊，Email: {}", MaskingUtils.maskEmail(email));
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("找不到使用者，Email: {}", MaskingUtils.maskEmail(email));
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        LocalDateTime lastLoginAt = loginHistoryService.getLastLoginTime(user.getId());
        
        if (lastLoginAt == null) {
            lastLoginAt = user.getLastLoginAt();
            log.debug("Redis 無登入記錄，使用資料庫值，User ID: {}", user.getId());
        } else {
            log.debug("從 Redis ZSet 取得登入時間，User ID: {}", user.getId());
        }
        
        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                lastLoginAt
        );
        
        log.info("成功取得使用者資訊，使用者 ID: {}", user.getId());
        return userInfo;
    }

    /**
     * 取得使用者最後登入時間
     * 
     * <p>優先從 Redis ZSet 讀取，若 Redis 無資料則從資料庫讀取
     * 
     * @param email 使用者 Email 地址
     * @return LocalDateTime 最後登入時間，若從未登入則返回 null
     * @throws BusinessException 當使用者不存在時拋出 USER_NOT_FOUND
     */
    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getLastLoginTime(String email) {
        log.debug("取得最後登入時間，Email: {}", MaskingUtils.maskEmail(email));
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("找不到使用者，Email: {}", MaskingUtils.maskEmail(email));
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        LocalDateTime lastLoginAt = loginHistoryService.getLastLoginTime(user.getId());
        
        if (lastLoginAt == null) {
            lastLoginAt = user.getLastLoginAt();
            log.debug("Redis 無登入記錄，使用資料庫值，User ID: {}", user.getId());
        } else {
            log.debug("從 Redis ZSet 取得登入時間，User ID: {}", user.getId());
        }
        
        if (lastLoginAt != null) {
            log.info("成功取得最後登入時間，使用者 ID {}: {}", 
                    user.getId(), lastLoginAt);
        } else {
            log.info("使用者 ID {} 從未登入過", user.getId());
        }
        
        return lastLoginAt;
    }
}
