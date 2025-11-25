package com.denden.auth.service;

import com.denden.auth.dto.UserInfo;

import java.time.LocalDateTime;

/**
 * 使用者服務介面
 * 
 * @author Timmy
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 取得當前使用者資訊
     * 
     * <p>根據使用者 Email 查詢使用者的基本資訊</p>
     * 
     * @param email 使用者 Email 地址
     * @return UserInfo 使用者資訊 DTO
     * @throws BusinessException 當使用者不存在時拋出 USER_NOT_FOUND
     */
    UserInfo getCurrentUserInfo(String email);

    /**
     * 取得使用者最後登入時間
     * 
     * <p>查詢指定使用者的最後登入時間記錄</p>
     * 
     * @param email 使用者 Email 地址
     * @return LocalDateTime 最後登入時間，若從未登入則返回 null
     * @throws BusinessException 當使用者不存在時拋出 USER_NOT_FOUND
     */
    LocalDateTime getLastLoginTime(String email);
}
