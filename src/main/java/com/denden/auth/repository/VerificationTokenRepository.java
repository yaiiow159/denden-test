package com.denden.auth.repository;

import com.denden.auth.entity.TokenType;
import com.denden.auth.entity.User;
import com.denden.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 驗證 Token 資料存取介面
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>根據 Token 字串查詢（Email 驗證、密碼重設）</li>
 *   <li>根據使用者和類型查詢（重新發送驗證郵件）</li>
 *   <li>清理過期 Token（定期維護）</li>
 * </ul>
 *
 * @author Member Auth System
 * @since 1.0.0
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * 根據 Token 字串查詢驗證 Token
     * <p>
     * 用於 Email 驗證和密碼重設流程
     * </p>
     *
     * @param token Token 字串（UUID）
     * @return Optional 包裝的驗證 Token，如果不存在則為 empty
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * 根據使用者和 Token 類型查詢驗證 Token
     * <p>
     * 用於重新發送驗證郵件時，查詢使用者現有的 Token
     * 可能返回多個 Token（例如使用者多次請求重發）
     * </p>
     *
     * @param user 使用者實體
     * @param type Token 類型
     * @return 符合條件的驗證 Token 列表
     */
    List<VerificationToken> findByUserAndType(User user, TokenType type);

    /**
     * 根據使用者、類型和未使用狀態查詢驗證 Token
     * <p>
     * 用於查詢使用者尚未使用的特定類型 Token
     * 例如：檢查是否有未使用的 Email 驗證 Token
     * </p>
     *
     * @param user 使用者實體
     * @param type Token 類型
     * @param used 是否已使用
     * @return Optional 包裝的驗證 Token，如果不存在則為 empty
     */
    Optional<VerificationToken> findByUserAndTypeAndUsed(User user, TokenType type, boolean used);

    /**
     * 根據使用者查詢所有驗證 Token
     * <p>
     * 用於管理功能，查看使用者的所有 Token 記錄
     * </p>
     *
     * @param user 使用者實體
     * @return 該使用者的所有驗證 Token 列表
     */
    List<VerificationToken> findByUser(User user);

    /**
     * 刪除過期的驗證 Token
     * <p>
     * 用於定期清理過期 Token，避免資料表過大
     * </p>
     *
     * @param now 當前時間
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 刪除已使用且建立時間超過指定天數的 Token
     * <p>
     * 用於清理舊的已使用 Token，保留一定時間的歷史記錄
     * </p>
     *
     * @param cutoffDate 截止日期（早於此日期的記錄將被刪除）
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.used = true AND vt.createdAt < :cutoffDate")
    int deleteUsedTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 統計特定類型的有效 Token 數量
     * <p>
     * 用於監控和統計，例如：
     * <ul>
     *   <li>統計待驗證的 Email 數量</li>
     *   <li>統計待重設的密碼數量</li>
     * </ul>
     * </p>
     *
     * @param type Token 類型
     * @param now  當前時間
     * @return 有效 Token 數量
     */
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.type = :type AND vt.used = false AND vt.expiresAt > :now")
    long countValidTokensByType(@Param("type") TokenType type, @Param("now") LocalDateTime now);
}
