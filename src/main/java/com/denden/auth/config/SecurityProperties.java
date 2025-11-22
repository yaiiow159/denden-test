package com.denden.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全相關配置屬性
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();
    private Otp otp = new Otp();
    private VerificationToken verificationToken = new VerificationToken();
    private AccountLock accountLock = new AccountLock();
    
    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Long expirationMs;
        private String issuer;
    }
    
    @Getter
    @Setter
    public static class RateLimit {
        private Integer maxRequests;
        private Integer windowSeconds;
    }
    
    @Getter
    @Setter
    public static class Otp {
        private Integer length;
        private Integer expirationSeconds;
        private Integer maxAttempts;
    }
    
    @Getter
    @Setter
    public static class VerificationToken {
        private Integer expirationHours;
    }
    
    @Getter
    @Setter
    public static class AccountLock {
        private Integer maxFailedAttempts;
        private Integer lockDurationMinutes;
    }
}
