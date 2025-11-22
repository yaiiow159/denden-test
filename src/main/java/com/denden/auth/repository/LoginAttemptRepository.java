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
 * <p>主要功能：</p>
 * <ul>
 *   <li>統計時間範圍內的失敗次數（帳號鎖定機制）</li>
 *   <li>查詢特定 Email 的登入歷史（安全審計）</li>
 *   <li>清理舊的登入記錄（定期維護）</li>
 * </ul>
 *
 * @author Member Auth System
 * @since 1.0.0
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * 統計指定時間之後特定 Email 的登入嘗試次數
     * <p>
     * 用於帳號鎖定機制，統計最近時間範圍內的失敗次數
     * 例如：統計最近 30 分鐘內的失敗次數
     * </p>
     *
     * @param email 使用者 Email
     * @param after 起始時間（包含）
     * @return 登入嘗試次數
     */
    long countByEmailAndAttemptedAtAfter(String email, LocalDateTime after);

    /**
     * 統計指定時間之後特定 Email 的失敗登入次數
     * <p>
     * 用於帳號鎖定機制的核心查詢
     * 統計最近時間範圍內的失敗次數，達到閾值則鎖定帳號
     * </p>
     *
     * @param email      使用者 Email
     * @param successful 是否成功（false 表示失敗）
     * @param after      起始時間（包含）
     * @return 失敗登入次數
     */
    long countByEmailAndSuccessfulAndAttemptedAtAfter(String email, boolean successful, LocalDateTime after);

    /**
     * 查詢特定 Email 在指定時間範圍內的所有登入嘗試
     * <p>
     * 用於安全審計和異常行為分析
     * 按時間倒序排列，最新的記錄在前
     * </p>
     *
     * @param email 使用者 Email
     * @param after 起始時間（包含）
     * @return 登入嘗試記錄列表，按時間倒序
     */
    List<LoginAttempt> findByEmailAndAttemptedAtAfterOrderByAttemptedAtDesc(String email, LocalDateTime after);

    /**
     * 查詢特定 Email 的最近 N 次登入嘗試
     * <p>
     * 用於顯示使用者的登入歷史
     * 使用原生查詢限制返回數量，提升效能
     * </p>
     *
     * @param email 使用者 Email
     * @param limit 返回記錄數量
     * @return 最近的登入嘗試記錄列表
     */
    @Query(value = "SELECT * FROM login_attempts WHERE email = :email ORDER BY attempted_at DESC LIMIT :limit", nativeQuery = true)
    List<LoginAttempt> findRecentAttemptsByEmail(@Param("email") String email, @Param("limit") int limit);

    /**
     * 查詢特定 IP 地址在指定時間範圍內的登入嘗試
     * <p>
     * 用於偵測分散式攻擊
     * 例如：同一 IP 短時間內嘗試多個不同帳號
     * </p>
     *
     * @param ipAddress IP 地址
     * @param after     起始時間（包含）
     * @return 登入嘗試記錄列表
     */
    List<LoginAttempt> findByIpAddressAndAttemptedAtAfter(String ipAddress, LocalDateTime after);

    /**
     * 統計特定 IP 地址在指定時間範圍內的失敗次數
     * <p>
     * 用於 IP 級別的限流和封鎖
     * 例如：同一 IP 短時間內失敗次數過多則暫時封鎖
     * </p>
     *
     * @param ipAddress  IP 地址
     * @param successful 是否成功（false 表示失敗）
     * @param after      起始時間（包含）
     * @return 失敗次數
     */
    long countByIpAddressAndSuccessfulAndAttemptedAtAfter(String ipAddress, boolean successful, LocalDateTime after);

    /**
     * 刪除指定時間之前的登入嘗試記錄
     * <p>
     * 用於定期清理舊記錄，避免資料表過大
     * 保留 90 天內的記錄用於安全審計
     * 使用排程任務定期執行（例如每週一次）
     * </p>
     *
     * @param before 截止時間（早於此時間的記錄將被刪除）
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    int deleteOldAttempts(@Param("before") LocalDateTime before);

    /**
     * 統計指定時間範圍內的總登入嘗試次數
     * <p>
     * 用於系統監控和統計
     * </p>
     *
     * @param after 起始時間（包含）
     * @return 總登入嘗試次數
     */
    long countByAttemptedAtAfter(LocalDateTime after);

    /**
     * 統計指定時間範圍內的成功登入次數
     * <p>
     * 用於系統監控和統計
     * </p>
     *
     * @param successful 是否成功
     * @param after      起始時間（包含）
     * @return 成功登入次數
     */
    long countBySuccessfulAndAttemptedAtAfter(boolean successful, LocalDateTime after);
}
