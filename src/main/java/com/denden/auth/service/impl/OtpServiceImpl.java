package com.denden.auth.service.impl;

import com.denden.auth.entity.OtpSession;
import com.denden.auth.repository.OtpSessionRepository;
import com.denden.auth.service.OtpService;
import com.denden.auth.util.MaskingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * OTP 服務實作
 * 
 * @author Timmy
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private static final String OTP_EMAIL_KEY_PREFIX = "otp:email:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OtpSessionRepository otpSessionRepository;
    
    @Value("${app.security.otp.length:6}")
    private int otpLength;
    
    @Value("${app.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;
    
    @Value("${app.security.otp.max-attempts:3}")
    private int maxAttempts;
    
    @Override
    public String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }
    
    @Override
    @Transactional
    public void createOtpSession(String email, String otp) {
        boolean redisSuccess = createOtpSessionInRedis(email, otp);
        
        if (!redisSuccess) {
            log.warn("Redis 不可用，使用資料庫儲存 OTP，Email: {}", 
                    MaskingUtils.maskEmail(email));
            createOtpSessionInDatabase(email, otp);
        }
    }
    
    @Override
    @Transactional  
    public boolean verifyOtpByEmail(String email, String otp) {
        try {
            Boolean redisResult = verifyOtpInRedis(email, otp);
            if (redisResult != null) {
                return redisResult;
            }
        } catch (Exception e) {
            log.warn("Redis 驗證失敗，嘗試資料庫，Email: {}", 
                    MaskingUtils.maskEmail(email), e);
        }
        
        return verifyOtpInDatabase(email, otp);
    }
    
    @Override
    public boolean hasActiveSession(String email) {
        try {
            String redisKey = OTP_EMAIL_KEY_PREFIX + email;
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                return true;
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 連接失敗，檢查資料庫，Email: {}", 
                    MaskingUtils.maskEmail(email));
        }
        
        return otpSessionRepository.findLatestValidByEmail(email, LocalDateTime.now())
                .isPresent();
    }
    
    @Override
    @Transactional
    public void updateOtpSessionByEmail(String email, String newOtp) {
        boolean redisSuccess = updateOtpSessionInRedis(email, newOtp);
        
        if (!redisSuccess) {
            log.warn("Redis 不可用，使用資料庫更新 OTP，Email: {}", 
                    MaskingUtils.maskEmail(email));
            updateOtpSessionInDatabase(email, newOtp);
        }
    }
    
    
    private boolean createOtpSessionInRedis(String email, String otp) {
        try {
            String redisKey = OTP_EMAIL_KEY_PREFIX + email;
            
            OtpSession session = OtpSession.builder()
                    .email(email)
                    .otp(otp)
                    .attempts(0)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds))
                    .used(false)
                    .build();
            
            String sessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(
                    redisKey,
                    sessionJson,
                    Duration.ofSeconds(otpExpirationSeconds)
            );
            
            log.info("OTP session 已儲存到 Redis，Email: {}，TTL: {} 秒",
                    MaskingUtils.maskEmail(email), otpExpirationSeconds);
            return true;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 連接失敗，Email: {}", MaskingUtils.maskEmail(email), e);
            return false;
        } catch (JsonProcessingException e) {
            log.error("序列化 OTP session 失敗，Email: {}", MaskingUtils.maskEmail(email), e);
            return false;
        }
    }
    
    private Boolean verifyOtpInRedis(String email, String otp) {
        try {
            String redisKey = OTP_EMAIL_KEY_PREFIX + email;
            String sessionJson = redisTemplate.opsForValue().get(redisKey);
            
            if (sessionJson == null) {
                return null; 
            }
            
            OtpSession session = 
                    objectMapper.readValue(sessionJson, OtpSession.class);
            
            if (session.getAttempts() >= maxAttempts) {
                log.warn("OTP 驗證次數超過限制（Redis），Email: {}", 
                        MaskingUtils.maskEmail(email));
                redisTemplate.delete(redisKey);
                return false;
            }
            
            boolean isValid = otp != null && otp.equals(session.getOtp());
            
            if (isValid) {
                log.info("OTP 驗證成功（Redis），Email: {}", MaskingUtils.maskEmail(email));
                redisTemplate.delete(redisKey);
            } else {
                session.setAttempts(session.getAttempts() + 1);
                String updatedJson = objectMapper.writeValueAsString(session);
                Long ttl = redisTemplate.getExpire(redisKey);
                redisTemplate.opsForValue().set(
                        redisKey,
                        updatedJson,
                        Duration.ofSeconds(ttl != null && ttl > 0 ? ttl : otpExpirationSeconds)
                );
            }
            
            return isValid;
            
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 連接失敗，Email: {}", MaskingUtils.maskEmail(email));
            return null; 
        } catch (Exception e) {
            log.error("Redis 驗證異常，Email: {}", MaskingUtils.maskEmail(email), e);
            return null;
        }
    }
    
    private boolean updateOtpSessionInRedis(String email, String newOtp) {
        try {
            String redisKey = OTP_EMAIL_KEY_PREFIX + email;
            
            OtpSession session = OtpSession.builder()
                    .email(email)
                    .otp(newOtp)
                    .attempts(0)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds))
                    .used(false)
                    .build();
            
            String sessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(
                    redisKey,
                    sessionJson,
                    Duration.ofSeconds(otpExpirationSeconds)
            );
            
            log.info("OTP session 已更新到 Redis，Email: {}", MaskingUtils.maskEmail(email));
            return true;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 連接失敗，Email: {}", MaskingUtils.maskEmail(email), e);
            return false;
        } catch (JsonProcessingException e) {
            log.error("序列化 OTP session 失敗，Email: {}", MaskingUtils.maskEmail(email), e);
            return false;
        }
    }
    
    private void createOtpSessionInDatabase(String email, String otp) {
        otpSessionRepository.deleteByEmail(email);
        
        OtpSession session = OtpSession.builder()
                .email(email)
                .otp(otp)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds))
                .used(false)
                .build();
        
        otpSessionRepository.save(session);
        log.info("OTP session 已儲存到資料庫（備援），Email: {}", 
                MaskingUtils.maskEmail(email));
    }
    
    private boolean verifyOtpInDatabase(String email, String otp) {
        OtpSession session = otpSessionRepository
                .findLatestValidByEmail(email, LocalDateTime.now())
                .orElse(null);
        
        if (session == null) {
            log.warn("OTP session 不存在或已過期（資料庫），Email: {}", 
                    MaskingUtils.maskEmail(email));
            return false;
        }
        
        if (session.getAttempts() >= maxAttempts) {
            log.warn("OTP 驗證次數超過限制（資料庫），Email: {}", 
                    MaskingUtils.maskEmail(email));
            otpSessionRepository.delete(session);
            return false;
        }
        
        boolean isValid = otp != null && otp.equals(session.getOtp());
        
        if (isValid) {
            log.info("OTP 驗證成功（資料庫），Email: {}", MaskingUtils.maskEmail(email));
            session.setUsed(true);
            otpSessionRepository.save(session);
        } else {
            session.setAttempts(session.getAttempts() + 1);
            otpSessionRepository.save(session);
            log.warn("OTP 驗證失敗（資料庫），Email: {}，嘗試次數: {}", 
                    MaskingUtils.maskEmail(email), session.getAttempts());
        }
        
        return isValid;
    }
    
    private void updateOtpSessionInDatabase(String email, String newOtp) {
        otpSessionRepository.deleteByEmail(email);
        
        OtpSession session = OtpSession.builder()
                .email(email)
                .otp(newOtp)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds))
                .used(false)
                .build();
        
        otpSessionRepository.save(session);
        log.info("OTP session 已更新到資料庫（備援），Email: {}", 
                MaskingUtils.maskEmail(email));
    }
}
