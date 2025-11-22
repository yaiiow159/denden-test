package com.denden.auth.service.impl;

import com.denden.auth.model.OtpSession;
import com.denden.auth.service.OtpService;
import com.denden.auth.util.MaskingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * OTP 服務實作
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private static final String OTP_KEY_PREFIX = "otp:session:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
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
        
        String generatedOtp = otp.toString();
        log.debug("產生 OTP，長度: {}", otpLength);
        
        return generatedOtp;
    }
    
    @Override
    public String createOtpSession(String email, String otp) {
        String sessionId = UUID.randomUUID().toString();
        String redisKey = OTP_KEY_PREFIX + sessionId;
        
        OtpSession session = OtpSession.builder()
                .email(email)
                .otp(otp)
                .attempts(0)
                .createdAt(System.currentTimeMillis())
                .build();
        
        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(
                    redisKey,
                    sessionJson,
                    Duration.ofSeconds(otpExpirationSeconds)
            );
            
            log.info("建立 OTP session，Email: {}，sessionId: {}，TTL: {} 秒",
                    MaskingUtils.maskEmail(email), sessionId, otpExpirationSeconds);
            
            return sessionId;
            
        } catch (JsonProcessingException e) {
            log.error("序列化 OTP session 失敗，Email: {}", MaskingUtils.maskEmail(email), e);
            throw new RuntimeException("建立 OTP session 失敗", e);
        }
    }
    
    @Override
    public boolean validateOtp(String sessionId, String otp) {
        String redisKey = OTP_KEY_PREFIX + sessionId;
        String sessionJson = redisTemplate.opsForValue().get(redisKey);
        
        if (sessionJson == null) {
            log.warn("OTP session 不存在或已過期: {}", sessionId);
            return false;
        }
        
        try {
            OtpSession session = objectMapper.readValue(sessionJson, OtpSession.class);
            
            if (session.getAttempts() >= maxAttempts) {
                log.warn("OTP 驗證次數超過限制，session: {}，Email: {}",
                        sessionId, MaskingUtils.maskEmail(session.getEmail()));
                invalidateOtp(sessionId);
                return false;
            }
            
            boolean isValid = otp != null && otp.equals(session.getOtp());
            
            if (isValid) {
                log.info("OTP 驗證成功，session: {}，Email: {}",
                        sessionId, MaskingUtils.maskEmail(session.getEmail()));
            } else {
                log.warn("OTP 驗證失敗，session: {}，Email: {}",
                        sessionId, MaskingUtils.maskEmail(session.getEmail()));
            }
            
            return isValid;
            
        } catch (JsonProcessingException e) {
            log.error("反序列化 OTP session 失敗: {}", sessionId, e);
            return false;
        }
    }
    
    @Override
    public int incrementOtpAttempts(String sessionId) {
        String redisKey = OTP_KEY_PREFIX + sessionId;
        String sessionJson = redisTemplate.opsForValue().get(redisKey);
        
        if (sessionJson == null) {
            log.warn("無法增加嘗試次數 - OTP session 不存在: {}", sessionId);
            return 0;
        }
        
        try {
            OtpSession session = objectMapper.readValue(sessionJson, OtpSession.class);
            session.setAttempts(session.getAttempts() + 1);
            
            Long ttl = redisTemplate.getExpire(redisKey);
            if (ttl == null || ttl <= 0) {
                ttl = otpExpirationSeconds;
            }
            
            String updatedSessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(
                    redisKey,
                    updatedSessionJson,
                    Duration.ofSeconds(ttl)
            );
            
            log.info("OTP 嘗試次數增加至 {}，session: {}，Email: {}",
                    session.getAttempts(), sessionId, MaskingUtils.maskEmail(session.getEmail()));
            
            return session.getAttempts();
            
        } catch (JsonProcessingException e) {
            log.error("增加 OTP 嘗試次數失敗，session: {}", sessionId, e);
            return 0;
        }
    }
    
    @Override
    public void invalidateOtp(String sessionId) {
        String redisKey = OTP_KEY_PREFIX + sessionId;
        Boolean deleted = redisTemplate.delete(redisKey);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("OTP session 已失效: {}", sessionId);
        } else {
            log.warn("OTP session 失效失敗（可能不存在）: {}", sessionId);
        }
    }
    
    @Override
    public String getEmailFromSession(String sessionId) {
        String redisKey = OTP_KEY_PREFIX + sessionId;
        String sessionJson = redisTemplate.opsForValue().get(redisKey);
        
        if (sessionJson == null) {
            log.warn("OTP session 不存在: {}", sessionId);
            return null;
        }
        
        try {
            OtpSession session = objectMapper.readValue(sessionJson, OtpSession.class);
            return session.getEmail();
            
        } catch (JsonProcessingException e) {
            log.error("反序列化 OTP session 失敗: {}", sessionId, e);
            return null;
        }
    }
}
