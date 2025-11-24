package com.denden.auth.service.email;

import com.denden.auth.util.MaskingUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * JavaMail 郵件發送實現
 * 
 * @author Timmy
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "MAIL_PROVIDER", havingValue = "javamail")
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailSender implements EmailSender {
    
    private final JavaMailSender javaMailSender;
    
    @Value("${app.mail.javamail.username}")
    private String fromEmail;
    
    @Value("${app.mail.from-name:DenDen}")
    private String fromName;
    
    @Override
    public void send(String to, String subject, String htmlContent) throws EmailSendException {
        log.debug("使用 JavaMail 發送郵件至: {}", MaskingUtils.maskEmail(to));
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            javaMailSender.send(message);
            
            log.debug("JavaMail 郵件發送成功");
            
        } catch (Exception e) {
            log.error("JavaMail 郵件發送失敗: {}", e.getMessage(), e);
            throw new EmailSendException("JavaMail 郵件發送失敗", e);
        }
    }
    
    @Override
    public String getSenderType() {
        return "JAVAMAIL";
    }
}
