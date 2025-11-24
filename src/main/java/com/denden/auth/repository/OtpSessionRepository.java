package com.denden.auth.repository;

import com.denden.auth.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OTP Session Repository
 * 
 * @author Timmy
 * @since 2.0.0
 */
@Repository
public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {
    
    /**
     * 根據 Email 查找最新的有效 OTP Session
     */
    @Query("SELECT o FROM OtpSession o WHERE o.email = :email " +
           "AND o.used = false AND o.expiresAt > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpSession> findLatestValidByEmail(String email, LocalDateTime now);
    
    /**
     * 刪除過期的 OTP Sessions
     */
    @Modifying
    @Query("DELETE FROM OtpSession o WHERE o.expiresAt < :now")
    int deleteExpiredSessions(LocalDateTime now);
    
    /**
     * 刪除指定 Email 的所有 OTP Sessions
     */
    @Modifying
    @Query("DELETE FROM OtpSession o WHERE o.email = :email")
    int deleteByEmail(String email);
}
