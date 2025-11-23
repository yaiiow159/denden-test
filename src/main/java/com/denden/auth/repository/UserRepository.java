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
 * @author Member Auth System
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根據 Email 查詢使用者
     *
     * @param email 使用者 Email 地址
     * @return Optional 包裝的使用者，如果不存在則為 empty
     */
    Optional<User> findByEmail(String email);

    /**
     * 檢查 Email 是否已存在
     *
     * @param email 要檢查的 Email 地址
     * @return 如果 Email 已存在則返回 true
     */
    boolean existsByEmail(String email);

    /**
     * 根據帳號狀態查詢使用者列表
     *
     * @param status 帳號狀態
     * @return 符合狀態的使用者列表
     */
    List<User> findByStatus(AccountStatus status);

    /**
     * 根據 Email 和帳號狀態查詢使用者
     *
     * @param email  使用者 Email 地址
     * @param status 帳號狀態
     * @return Optional 包裝的使用者，如果不存在則為 empty
     */
    Optional<User> findByEmailAndStatus(String email, AccountStatus status);
}
