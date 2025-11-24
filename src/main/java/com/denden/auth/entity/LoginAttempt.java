package com.denden.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 登入嘗試記錄實體
 * 記錄所有登入嘗試
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_attempts_email", columnList = "email"),
    @Index(name = "idx_attempts_attempted_at", columnList = "attempted_at")
})
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Email 不可為空")
    private String email;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false)
    @NotNull(message = "登入結果不可為空")
    private boolean successful;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "attempted_at")
    private LocalDateTime attemptedAt;

    public LoginAttempt(String email, String ipAddress, boolean successful) {
        this.email = email;
        this.ipAddress = ipAddress;
        this.successful = successful;
    }

    public static LoginAttempt success(String email, String ipAddress) {
        return new LoginAttempt(email, ipAddress, true);
    }

    public static LoginAttempt failure(String email, String ipAddress) {
        return new LoginAttempt(email, ipAddress, false);
    }

    public boolean isFailed() {
        return !successful;
    }

    public boolean isAfter(LocalDateTime dateTime) {
        return this.attemptedAt.isAfter(dateTime);
    }

    public boolean isBefore(LocalDateTime dateTime) {
        return this.attemptedAt.isBefore(dateTime);
    }
}
