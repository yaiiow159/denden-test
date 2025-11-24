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
 * @author Timmy
 * @since 1.0.0
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * 根據 Token 字串查詢驗證 Token
     * 
     * @param token Token 字串（UUID）
     * @return Optional 包裝的驗證 Token，如果不存在則為 empty
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * 根據使用者和 Token 類型查詢驗證 Token
     *
     * @param user 使用者實體
     * @param type Token 類型
     * @return 符合條件的驗證 Token 列表
     */
    List<VerificationToken> findByUserAndType(User user, TokenType type);

    /**
     * 根據使用者、類型和未使用狀態查詢驗證 Token
     *
     * @param user 使用者實體
     * @param type Token 類型
     * @param used 是否已使用
     * @return Optional 包裝的驗證 Token，如果不存在則為 empty
     */
    Optional<VerificationToken> findByUserAndTypeAndUsed(User user, TokenType type, boolean used);

    /**
     * 根據使用者查詢所有驗證 Token
     *
     * @param user 使用者實體
     * @return 該使用者的所有驗證 Token 列表
     */
    List<VerificationToken> findByUser(User user);

    /**
     * 刪除過期的驗證 Token
     *
     * @param now 當前時間
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 刪除已使用且建立時間超過指定天數的 Token
     *
     * @param cutoffDate 截止日期（早於此日期的記錄將被刪除）
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.used = true AND vt.createdAt < :cutoffDate")
    int deleteUsedTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 統計特定類型的有效 Token 數量
     *
     * @param type Token 類型
     * @param now  當前時間
     * @return 有效 Token 數量
     */
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.type = :type AND vt.used = false AND vt.expiresAt > :now")
    long countValidTokensByType(@Param("type") TokenType type, @Param("now") LocalDateTime now);
    
    /**
     * 批次刪除過期的驗證 Token
     *
     * @param now 當前時間
     * @param limit 每次刪除的最大數量
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query(value = "DELETE FROM verification_tokens WHERE expires_at < :now LIMIT :limit", nativeQuery = true)
    int deleteExpiredTokensInBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);
    
    /**
     * 批次刪除已使用的舊 Token
     *
     * @param cutoffDate 截止日期
     * @param limit 每次刪除的最大數量
     * @return 刪除的記錄數量
     */
    @Modifying
    @Query(value = "DELETE FROM verification_tokens WHERE used = true AND created_at < :cutoffDate LIMIT :limit", nativeQuery = true)
    int deleteUsedTokensInBatch(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("limit") int limit);
}
