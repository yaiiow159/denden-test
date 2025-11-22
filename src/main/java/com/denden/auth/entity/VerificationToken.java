package com.denden.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 驗證 Token 實體
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "token"})
@Entity
@Table(name = "verification_tokens", indexes = {
    @Index(name = "idx_tokens_token", columnList = "token"),
    @Index(name = "idx_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_tokens_expires_at", columnList = "expires_at")
})
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    @NotBlank(message = "Token 不可為空")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "使用者不可為空")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Token 類型不可為空")
    private TokenType type;

    @Column(nullable = false, name = "expires_at")
    @NotNull(message = "過期時間不可為空")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    public VerificationToken(User user, TokenType type, LocalDateTime expiresAt) {
        this.token = UUID.randomUUID().toString();
        this.user = user;
        this.type = type;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public static VerificationToken createEmailVerificationToken(User user) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        return new VerificationToken(user, TokenType.EMAIL_VERIFICATION, expiresAt);
    }

    public static VerificationToken createPasswordResetToken(User user) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        return new VerificationToken(user, TokenType.PASSWORD_RESET, expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.used && !isExpired();
    }

    public boolean isEmailVerification() {
        return this.type == TokenType.EMAIL_VERIFICATION;
    }

    public boolean isPasswordReset() {
        return this.type == TokenType.PASSWORD_RESET;
    }
    
    @Override
    public String toString() {
        return "VerificationToken{" +
                "id=" + id +
                ", token='" + (token != null && token.length() > 8 ? token.substring(0, 8) + "..." : token) + "'" +
                ", type=" + type +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", createdAt=" + createdAt +
                '}';
    }
}
