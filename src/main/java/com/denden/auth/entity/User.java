package com.denden.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 使用者實體
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "email"})
@ToString(exclude = "passwordHash")
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status", columnList = "status")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "密碼雜湊值不可為空")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "帳號狀態不可為空")
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = AccountStatus.PENDING;
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    public void lock() {
        this.status = AccountStatus.LOCKED;
    }

    public void unlock() {
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
        }
    }

    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        return this.status == AccountStatus.LOCKED;
    }

    public boolean isPending() {
        return this.status == AccountStatus.PENDING;
    }
}
