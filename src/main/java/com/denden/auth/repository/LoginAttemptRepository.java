package com.denden.auth.repository;

import com.denden.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登入嘗試記錄資料存取介面
 *
 * @author Timmy
 * @since 1.0.0
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * 統計指定時間之後特定 Email 的登入嘗試次數
     *
     * @param email 使用者 Email
     * @param after 起始時間（包含）
     * @return 登入嘗試次數
     */
    long countByEmailAndAttemptedAtAfter(String email, LocalDateTime after);

    /**
     * 統計指定時間之後特定 Email 的失敗登入次數
     *
     * @param email      使用者 Email
     * @param successful 是否成功（false 表示失敗）
     * @param after      起始時間（包含）
     * @return 失敗登入次數
     */
    long countByEmailAndSuccessfulAndAttemptedAtAfter(String email, boolean successful, LocalDateTime after);

    /**
     * 查詢特定 Email 在指定時間範圍內的所有登入嘗試
     *
     * @param email 使用者 Email
     * @param after 起始時間（包含）
     * @return 登入嘗試記錄列表，按時間倒序
     */
    List<LoginAttempt> findByEmailAndAttemptedAtAfterOrderByAttemptedAtDesc(String email, LocalDateTime after);

    /**
     * 查詢特定 Email 的最近 N 次登入嘗試
     *
     * @param email 使用者 Email
     * @param limit 返回記錄數量
     * @return 最近的登入嘗試記錄列表
     */
    @Query(value = "SELECT * FROM login_attempts WHERE email = :email ORDER BY attempted_at DESC LIMIT :limit", nativeQuery = true)
    List<LoginAttempt> findRecentAttemptsByEmail(@Param("email") String email, @Param("limit") int limit);

    /**
     * 查詢特定 IP 地址在指定時間範圍內的登入嘗試
     *
     * @param ipAddress IP 地址
     * @param after     起始時間（包含）
     * @return 登入嘗試記錄列表
     */
    List<LoginAttempt> findByIpAddressAndAttemptedAtAfter(String ipAddress, LocalDateTime after);

    /**
     * 統計特定 IP 地址在指定時間範圍內的失敗次數
     *
     * @param ipAddress  IP 地址
     * @param successful 是否成功（false 表示失敗）
     * @param after      起始時間（包含）
     * @return 失敗次數
     */
    long countByIpAddressAndSuccessfulAndAttemptedAtAfter(String ipAddress, boolean successful, LocalDateTime after);

    /**
     * 刪除指定時間之前的登入嘗試記錄
     *
     * @param before 截止時間（早於此時間的記錄將被刪除）
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    int deleteOldAttempts(@Param("before") LocalDateTime before);

    /**
     * 統計指定時間範圍內的總登入嘗試次數
     * 
     * @param after 起始時間（包含）
     * @return 總登入嘗試次數
     */
    long countByAttemptedAtAfter(LocalDateTime after);

    /**
     * 統計指定時間範圍內的成功登入次數
     *
     * @param successful 是否成功
     * @param after      起始時間（包含）
     * @return 成功登入次數
     */
    long countBySuccessfulAndAttemptedAtAfter(boolean successful, LocalDateTime after);
    
    /**
     * 批次刪除舊的登入嘗試記錄
     * <p>
     * 使用 LIMIT 限制每次刪除的數量，避免長時間鎖表
     * </p>
     *
     * @param before 截止時間
     * @param limit 每次刪除的最大數量
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query(value = "DELETE FROM login_attempts WHERE attempted_at < :before LIMIT :limit", nativeQuery = true)
    int deleteOldAttemptsInBatch(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
