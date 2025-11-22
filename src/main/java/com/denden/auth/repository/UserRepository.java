package com.denden.auth.repository;

import com.denden.auth.entity.AccountStatus;
import com.denden.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 使用者資料存取介面
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>根據 Email 查詢使用者（登入、註冊檢查）</li>
 *   <li>檢查 Email 是否已存在（註冊驗證）</li>
 *   <li>根據帳號狀態查詢使用者（管理功能）</li>
 * </ul>
 *
 * @author Member Auth System
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根據 Email 查詢使用者
     * <p>
     * 用於登入驗證、註冊檢查等場景
     * </p>
     *
     * @param email 使用者 Email 地址
     * @return Optional 包裝的使用者，如果不存在則為 empty
     */
    Optional<User> findByEmail(String email);

    /**
     * 檢查 Email 是否已存在
     * <p>
     * 用於註冊時驗證 Email 是否已被使用
     * </p>
     *
     * @param email 要檢查的 Email 地址
     * @return 如果 Email 已存在則返回 true
     */
    boolean existsByEmail(String email);

    /**
     * 根據帳號狀態查詢使用者列表
     * <p>
     * 用於管理功能，例如：
     * <ul>
     *   <li>查詢所有待驗證帳號</li>
     *   <li>查詢所有被鎖定帳號</li>
     *   <li>查詢所有啟用帳號</li>
     * </ul>
     *
     * @param status 帳號狀態
     * @return 符合狀態的使用者列表
     */
    List<User> findByStatus(AccountStatus status);

    /**
     * 根據 Email 和帳號狀態查詢使用者
     * <p>
     * 組合查詢，用於特定場景
     * 例如：檢查特定 Email 的帳號是否為啟用狀態
     * </p>
     *
     * @param email  使用者 Email 地址
     * @param status 帳號狀態
     * @return Optional 包裝的使用者，如果不存在則為 empty
     */
    Optional<User> findByEmailAndStatus(String email, AccountStatus status);
}
