package com.denden.auth.service.impl;

import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.service.EmailService;
import com.denden.auth.service.email.EmailSendException;
import com.denden.auth.service.email.EmailSenderFactory;
import com.denden.auth.util.EmailTemplateLoader;
import com.denden.auth.util.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Email 發送服務實作
 * 
 * <p>採用策略模式設計，支援動態切換不同的郵件發送渠道。
 * 通過 EmailSenderFactory 獲取當前配置的發送器實現。
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final EmailSenderFactory emailSenderFactory;
    private final EmailTemplateLoader templateLoader;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Override
    @Async
    @Retryable(
        retryFor = {EmailSendException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendVerificationEmail(String to, String token) {
        log.info("準備發送驗證郵件至: {} (使用: {})", 
            MaskingUtils.maskEmail(to), 
            emailSenderFactory.getActiveSender().getSenderType());
        
        String verificationLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String subject = "驗證您的帳號 - Member Auth System";
        String htmlContent = buildVerificationEmailHtml(verificationLink);
        
        try {
            emailSenderFactory.getActiveSender().send(to, subject, htmlContent);
            log.info("驗證郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (EmailSendException e) {
            log.error("驗證郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "驗證郵件發送失敗", e);
        }
    }
    
    @Override
    @Async
    @Retryable(
        retryFor = {EmailSendException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendOtpEmail(String to, String otp) {
        log.info("準備發送 OTP 郵件至: {} (使用: {})", 
            MaskingUtils.maskEmail(to),
            emailSenderFactory.getActiveSender().getSenderType());
        
        String subject = "您的登入驗證碼 - Member Auth System";
        String htmlContent = buildOtpEmailHtml(otp);
        
        try {
            emailSenderFactory.getActiveSender().send(to, subject, htmlContent);
            log.info("OTP 郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (EmailSendException e) {
            log.error("OTP 郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "OTP 郵件發送失敗", e);
        }
    }
    
    @Override
    @Async
    @Retryable(
        retryFor = {EmailSendException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendAccountLockedEmail(String to) {
        log.info("準備發送帳號鎖定通知郵件至: {} (使用: {})", 
            MaskingUtils.maskEmail(to),
            emailSenderFactory.getActiveSender().getSenderType());
        
        String subject = "帳號安全通知 - 帳號已被暫時鎖定";
        String htmlContent = buildAccountLockedEmailHtml();
        
        try {
            emailSenderFactory.getActiveSender().send(to, subject, htmlContent);
            log.info("帳號鎖定通知郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (EmailSendException e) {
            log.error("帳號鎖定通知郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "帳號鎖定通知郵件發送失敗", e);
        }
    }
    
    private String buildVerificationEmailHtml(String verificationLink) {
        return templateLoader.loadTemplate(
            "verification-email.html",
            Map.of("VERIFICATION_LINK", verificationLink)
        );
    }
    
    private String buildOtpEmailHtml(String otp) {
        return templateLoader.loadTemplate(
            "otp-email.html",
            Map.of("OTP_CODE", otp)
        );
    }
    
    private String buildAccountLockedEmailHtml() {
        return templateLoader.loadTemplate("account-locked-email.html");
    }
}
