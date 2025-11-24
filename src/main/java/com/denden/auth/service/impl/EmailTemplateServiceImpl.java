package com.denden.auth.service.impl;

import com.denden.auth.service.EmailTemplateService;
import com.denden.auth.util.EmailTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 郵件模板服務實作
 * 
 * 使用模板引擎處理變數替換
 * 
 * @author Timmy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {
    
    private final EmailTemplateLoader templateLoader;
    
    @Override
    public String buildVerificationEmail(String verificationLink) {
        log.debug("構建驗證郵件模板");
        return templateLoader.loadTemplate(
            "verification-email.html",
            Map.of("VERIFICATION_LINK", verificationLink)
        );
    }
    
    @Override
    public String buildOtpEmail(String otp) {
        log.debug("構建 OTP 郵件模板");
        return templateLoader.loadTemplate(
            "otp-email.html",
            Map.of("OTP_CODE", otp)
        );
    }
    
    @Override
    public String buildAccountLockedEmail() {
        log.debug("構建帳號鎖定通知郵件模板");
        return templateLoader.loadTemplate("account-locked-email.html");
    }
    
    @Override
    public String buildWelcomeEmail(String username) {
        log.debug("構建歡迎郵件模板，使用者: {}", username);
        return templateLoader.loadTemplate(
            "welcome-email.html",
            Map.of("USERNAME", username)
        );
    }
}
