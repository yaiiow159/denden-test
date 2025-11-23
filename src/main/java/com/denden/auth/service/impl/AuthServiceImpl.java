package com.denden.auth.service.impl;

import com.denden.auth.config.SecurityProperties;
import com.denden.auth.dto.AuthResponse;
import com.denden.auth.dto.LoginRequest;
import com.denden.auth.dto.OtpResponse;
import com.denden.auth.dto.RegisterRequest;
import com.denden.auth.dto.UserInfo;
import com.denden.auth.dto.VerifyOtpRequest;
import com.denden.auth.entity.AccountStatus;
import com.denden.auth.entity.LoginAttempt;
import com.denden.auth.entity.TokenType;
import com.denden.auth.entity.User;
import com.denden.auth.entity.VerificationToken;
import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.repository.LoginAttemptRepository;
import com.denden.auth.repository.UserRepository;
import com.denden.auth.repository.VerificationTokenRepository;
import com.denden.auth.service.AuthService;
import com.denden.auth.service.EmailService;
import com.denden.auth.service.LoginHistoryService;
import com.denden.auth.service.OtpService;
import com.denden.auth.service.TokenService;
import com.denden.auth.util.MaskingUtils;
import com.denden.auth.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 認證服務實作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final LoginHistoryService loginHistoryService;
    private final SecurityProperties securityProperties;
    private final StringRedisTemplate redisTemplate;
    
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("開始處理會員註冊請求，Email: {}", MaskingUtils.maskEmail(request.email()));
        
        PasswordValidator.ValidationResult validationResult = PasswordValidator.validate(request.password());
        if (!validationResult.isValid()) {
            log.warn("密碼強度不符合要求，Email: {}", MaskingUtils.maskEmail(request.email()));
            throw new BusinessException(ErrorCode.WEAK_PASSWORD, validationResult.getFirstError());
        }
        
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Email 已被註冊，Email: {}", MaskingUtils.maskEmail(request.email()));
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        String passwordHash = passwordEncoder.encode(request.password());
        
        User user = new User(request.email(), passwordHash);
        user = userRepository.save(user);
        log.info("使用者建立成功，User ID: {}, Email: {}", user.getId(), MaskingUtils.maskEmail(user.getEmail()));
        
        VerificationToken verificationToken = VerificationToken.createEmailVerificationToken(user);
        verificationToken = verificationTokenRepository.save(verificationToken);
        log.info("驗證 Token 建立成功，Token ID: {}, 過期時間: {}", 
                verificationToken.getId(), verificationToken.getExpiresAt());
        
        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
            log.info("驗證郵件已發送，Email: {}", MaskingUtils.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("發送驗證郵件失敗，Email: {}, 錯誤: {}", 
                    MaskingUtils.maskEmail(user.getEmail()), e.getMessage(), e);
        }
        
        log.info("會員註冊流程完成，User ID: {}", user.getId());
    }
    
    @Override
    @Transactional
    public void verifyEmail(String token) {
        log.info("開始處理 Email 驗證請求，Token: {}...", MaskingUtils.maskToken(token));
        
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("驗證 Token 不存在，Token: {}...", MaskingUtils.maskToken(token));
                    return new BusinessException(ErrorCode.TOKEN_NOT_FOUND);
                });
        
        if (!verificationToken.isEmailVerification()) {
            log.warn("Token 類型不正確，期望: EMAIL_VERIFICATION，實際: {}", verificationToken.getType());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        if (verificationToken.isUsed()) {
            log.warn("驗證 Token 已被使用，Token ID: {}", verificationToken.getId());
            throw new BusinessException(ErrorCode.TOKEN_ALREADY_USED);
        }
        
        if (verificationToken.isExpired()) {
            log.warn("驗證 Token 已過期，Token ID: {}, 過期時間: {}", 
                    verificationToken.getId(), verificationToken.getExpiresAt());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        User user = verificationToken.getUser();
        user.activate();
        userRepository.save(user);
        log.info("使用者帳號已啟用，User ID: {}, Email: {}", user.getId(), MaskingUtils.maskEmail(user.getEmail()));
        
        verificationToken.markAsUsed();
        verificationTokenRepository.save(verificationToken);
        log.info("驗證 Token 已標記為已使用，Token ID: {}", verificationToken.getId());
        log.info("Email 驗證流程完成，User ID: {}", user.getId());
    }
    
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        log.info("開始處理重新發送驗證郵件請求，Email: {}", MaskingUtils.maskEmail(email));
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("使用者不存在，Email: {}", MaskingUtils.maskEmail(email));
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        if (user.getStatus() != AccountStatus.PENDING) {
            log.warn("帳號狀態不是 PENDING，無法重新發送驗證郵件，User ID: {}, Status: {}", 
                    user.getId(), user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED, 
                    "帳號狀態不正確，無法重新發送驗證郵件");
        }
        
        VerificationToken verificationToken = VerificationToken.createEmailVerificationToken(user);
        verificationToken = verificationTokenRepository.save(verificationToken);
        log.info("新的驗證 Token 建立成功，Token ID: {}, 過期時間: {}", 
                verificationToken.getId(), verificationToken.getExpiresAt());
        
        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
            log.info("驗證郵件已重新發送，Email: {}", MaskingUtils.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("重新發送驗證郵件失敗，Email: {}, 錯誤: {}", 
                    MaskingUtils.maskEmail(user.getEmail()), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR);
        }
        
        log.info("重新發送驗證郵件流程完成，User ID: {}", user.getId());
    }
    
    @Override
    @Transactional
    public OtpResponse login(LoginRequest request, String ipAddress) {
        log.info("開始處理登入請求，Email: {}, IP: {}", MaskingUtils.maskEmail(request.email()), ipAddress);
        
        if (isAccountLocked(request.email())) {
            log.warn("帳號已被鎖定，Email: {}", MaskingUtils.maskEmail(request.email()));
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    recordLoginAttempt(request.email(), ipAddress, false);
                    log.warn("使用者不存在，Email: {}", MaskingUtils.maskEmail(request.email()));
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });
        
        if (!user.isActive()) {
            recordLoginAttempt(request.email(), ipAddress, false);
            if (user.isPending()) {
                log.warn("帳號尚未啟用，Email: {}", MaskingUtils.maskEmail(request.email()));
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
            } else if (user.isLocked()) {
                log.warn("帳號已被鎖定，Email: {}", MaskingUtils.maskEmail(request.email()));
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
            }
            log.warn("帳號狀態異常，Email: {}, Status: {}", MaskingUtils.maskEmail(request.email()), user.getStatus());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLoginAttempt(request.email(), ipAddress, false);
            log.warn("密碼驗證失敗，Email: {}", MaskingUtils.maskEmail(request.email()));
            
            checkAndLockAccount(request.email());
            
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        recordLoginAttempt(request.email(), ipAddress, true);
        log.info("密碼驗證成功，Email: {}", MaskingUtils.maskEmail(request.email()));
        
        String otp = otpService.generateOtp();
        String sessionId = otpService.createOtpSession(request.email(), otp);
        log.info("OTP 已產生，Session ID: {}, Email: {}", sessionId, MaskingUtils.maskEmail(request.email()));
        
        try {
            emailService.sendOtpEmail(request.email(), otp);
            log.info("OTP 郵件已發送，Email: {}", MaskingUtils.maskEmail(request.email()));
        } catch (Exception e) {
            log.error("發送 OTP 郵件失敗，Email: {}, 錯誤: {}", 
                    MaskingUtils.maskEmail(request.email()), e.getMessage(), e);
        }
        
        log.info("登入第一階段完成，Email: {}", MaskingUtils.maskEmail(request.email()));
        
        return OtpResponse.of(sessionId, (long) securityProperties.getOtp().getExpirationSeconds());
    }
    
    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        log.info("開始處理 OTP 驗證請求，Session ID: {}", request.sessionId());
        
        String email = otpService.getEmailFromSession(request.sessionId());
        if (email == null) {
            log.warn("OTP 會話不存在或已過期，Session ID: {}", request.sessionId());
            throw new BusinessException(ErrorCode.OTP_SESSION_NOT_FOUND);
        }
        
        boolean isValid = otpService.validateOtp(request.sessionId(), request.otp());
        
        if (!isValid) {
            int attempts = otpService.incrementOtpAttempts(request.sessionId());
            log.warn("OTP 驗證失敗，Session ID: {}, Email: {}, 嘗試次數: {}", 
                    request.sessionId(), MaskingUtils.maskEmail(email), attempts);
            
            int maxAttempts = securityProperties.getOtp().getMaxAttempts();
            if (attempts >= maxAttempts) {
                log.warn("OTP 驗證失敗次數超過限制，Session ID: {}, Email: {}", 
                        request.sessionId(), MaskingUtils.maskEmail(email));
                otpService.invalidateOtp(request.sessionId());
                throw new BusinessException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
            }
            
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("使用者不存在，Email: {}", MaskingUtils.maskEmail(email));
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        LocalDateTime loginTime = LocalDateTime.now();
        user.setLastLoginAt(loginTime);
        userRepository.save(user);
        log.info("使用者最後登入時間已更新到資料庫，User ID: {}, Email: {}", 
                user.getId(), MaskingUtils.maskEmail(user.getEmail()));
        
        loginHistoryService.recordLoginTime(user.getId(), loginTime);
        log.info("使用者登入時間已記錄到 Redis ZSet，User ID: {}", user.getId());
        
        String jwtToken = tokenService.generateJwtToken(user);
        log.info("JWT Token 已產生，User ID: {}, Email: {}", 
                user.getId(), MaskingUtils.maskEmail(user.getEmail()));
        
        otpService.invalidateOtp(request.sessionId());
        log.info("OTP 會話已刪除，Session ID: {}", request.sessionId());
        
        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getLastLoginAt()
        );
        
        Long expiresIn = securityProperties.getJwt().getExpirationMs() / 1000;
        
        log.info("OTP 驗證流程完成，User ID: {}, Email: {}", user.getId(), MaskingUtils.maskEmail(user.getEmail()));
        
        return AuthResponse.bearer(jwtToken, expiresIn, userInfo);
    }
    
    @Override
    @Transactional
    public OtpResponse resendOtp(String sessionId) {
        log.info("開始處理重新發送 OTP 請求，Session ID: {}", sessionId);
        
        String email = otpService.getEmailFromSession(sessionId);
        if (email == null) {
            log.warn("OTP 會話不存在或已過期，Session ID: {}", sessionId);
            throw new BusinessException(ErrorCode.OTP_SESSION_NOT_FOUND);
        }
        
        String newOtp = otpService.generateOtp();
        log.info("新的 OTP 已產生，Email: {}", MaskingUtils.maskEmail(email));
        
        otpService.invalidateOtp(sessionId);
        
        String newSessionId = otpService.createOtpSession(email, newOtp);
        log.info("新的 OTP 會話已建立，Session ID: {}, Email: {}", newSessionId, MaskingUtils.maskEmail(email));
        
        try {
            emailService.sendOtpEmail(email, newOtp);
            log.info("新的 OTP 郵件已發送，Email: {}", MaskingUtils.maskEmail(email));
        } catch (Exception e) {
            log.error("發送 OTP 郵件失敗，Email: {}, 錯誤: {}", 
                    MaskingUtils.maskEmail(email), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR);
        }
        
        log.info("重新發送 OTP 流程完成，Email: {}", MaskingUtils.maskEmail(email));
        
        return OtpResponse.of(newSessionId, (long) securityProperties.getOtp().getExpirationSeconds());
    }
    
    /**
     * 記錄登入嘗試
     */
    private void recordLoginAttempt(String email, String ipAddress, boolean successful) {
        try {
            LoginAttempt attempt = new LoginAttempt(email, ipAddress, successful);
            loginAttemptRepository.save(attempt);
            log.debug("登入嘗試已記錄，Email: {}, IP: {}, 成功: {}", 
                    MaskingUtils.maskEmail(email), ipAddress, successful);
        } catch (Exception e) {
            log.error("記錄登入嘗試失敗，Email: {}, 錯誤: {}", 
                    MaskingUtils.maskEmail(email), e.getMessage(), e);
        }
    }
    
    /**
     * 檢查帳號是否被鎖定
     */
    private boolean isAccountLocked(String email) {
        String lockKey = "account_lock:" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
    
    /**
     * 檢查並鎖定帳號
     */
    private void checkAndLockAccount(String email) {
        int maxFailedAttempts = securityProperties.getAccountLock().getMaxFailedAttempts();
        int lockDurationMinutes = securityProperties.getAccountLock().getLockDurationMinutes();
        
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        long failedAttempts = loginAttemptRepository.countByEmailAndSuccessfulAndAttemptedAtAfter(
                email, false, thirtyMinutesAgo);
        
        log.debug("最近 30 分鐘內失敗次數，Email: {}, 次數: {}", MaskingUtils.maskEmail(email), failedAttempts);
        
        if (failedAttempts >= maxFailedAttempts) {
            lockAccount(email, lockDurationMinutes);
            log.warn("帳號已被鎖定，Email: {}, 失敗次數: {}", MaskingUtils.maskEmail(email), failedAttempts);
            
            try {
                emailService.sendAccountLockedEmail(email);
                log.info("帳號鎖定通知郵件已發送，Email: {}", MaskingUtils.maskEmail(email));
            } catch (Exception e) {
                log.error("發送帳號鎖定通知郵件失敗，Email: {}, 錯誤: {}", 
                        MaskingUtils.maskEmail(email), e.getMessage(), e);
            }
        }
    }
    
    /**
     * 鎖定帳號
     */
    private void lockAccount(String email, int durationMinutes) {
        String lockKey = "account_lock:" + email;
        redisTemplate.opsForValue().set(lockKey, "locked", durationMinutes, TimeUnit.MINUTES);
        log.info("帳號已鎖定 {} 分鐘，Email: {}", durationMinutes, MaskingUtils.maskEmail(email));
    }
}
