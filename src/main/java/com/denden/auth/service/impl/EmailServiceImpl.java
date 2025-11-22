package com.denden.auth.service.impl;

import com.denden.auth.config.MailjetConfig;
import com.denden.auth.exception.BusinessException;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.service.EmailService;
import com.denden.auth.util.EmailTemplateLoader;
import com.denden.auth.util.MaskingUtils;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Email 發送服務實作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final MailjetClient mailjetClient;
    private final MailjetConfig mailjetConfig;
    private final EmailTemplateLoader templateLoader;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Override
    @Async
    @Retryable(
        retryFor = {MailjetException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendVerificationEmail(String to, String token) {
        log.info("準備發送驗證郵件至: {}", MaskingUtils.maskEmail(to));
        
        String verificationLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String subject = "驗證您的帳號 - Member Auth System";
        String htmlContent = buildVerificationEmailHtml(verificationLink);
        
        try {
            sendEmail(to, subject, htmlContent);
            log.info("驗證郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (MailjetException e) {
            log.error("驗證郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "驗證郵件發送失敗", e);
        }
    }
    
    @Override
    @Async
    @Retryable(
        retryFor = {MailjetException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendOtpEmail(String to, String otp) {
        log.info("準備發送 OTP 郵件至: {}", MaskingUtils.maskEmail(to));
        
        String subject = "您的登入驗證碼 - Member Auth System";
        String htmlContent = buildOtpEmailHtml(otp);
        
        try {
            sendEmail(to, subject, htmlContent);
            log.info("OTP 郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (MailjetException e) {
            log.error("OTP 郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "OTP 郵件發送失敗", e);
        }
    }
    
    @Override
    @Async
    @Retryable(
        retryFor = {MailjetException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendAccountLockedEmail(String to) {
        log.info("準備發送帳號鎖定通知郵件至: {}", MaskingUtils.maskEmail(to));
        
        String subject = "帳號安全通知 - 帳號已被暫時鎖定";
        String htmlContent = buildAccountLockedEmailHtml();
        
        try {
            sendEmail(to, subject, htmlContent);
            log.info("帳號鎖定通知郵件發送成功至: {}", MaskingUtils.maskEmail(to));
        } catch (MailjetException e) {
            log.error("帳號鎖定通知郵件發送失敗至: {}, 錯誤: {}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMAIL_SERVICE_ERROR, "帳號鎖定通知郵件發送失敗", e);
        }
    }
    
    private void sendEmail(String to, String subject, String htmlContent) throws MailjetException {
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
            .property(Emailv31.MESSAGES, new JSONArray()
                .put(new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                        .put("Email", mailjetConfig.getFromEmail())
                        .put("Name", mailjetConfig.getFromName()))
                    .put(Emailv31.Message.TO, new JSONArray()
                        .put(new JSONObject()
                            .put("Email", to)))
                    .put(Emailv31.Message.SUBJECT, subject)
                    .put(Emailv31.Message.HTMLPART, htmlContent)));
        
        MailjetResponse response = mailjetClient.post(request);
        
        if (response.getStatus() != 200) {
            log.error("Mailjet API 回應錯誤，狀態碼: {}, 回應: {}", 
                response.getStatus(), response.getData());
            throw new MailjetException("Mailjet API 回應錯誤: " + response.getStatus());
        }
        
        log.debug("Mailjet API 回應成功，狀態碼: {}", response.getStatus());
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
