package com.denden.auth.service.email;

import com.denden.auth.config.MailjetConfig;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mailjet 郵件發送實現
 * 
 * <p>使用 Mailjet API 發送郵件的策略實現。
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "mailjet", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MailjetEmailSender implements EmailSender {
    
    private final MailjetClient mailjetClient;
    private final MailjetConfig mailjetConfig;
    
    @Override
    public void send(String to, String subject, String htmlContent) throws EmailSendException {
        log.debug("使用 Mailjet 發送郵件至: {}", MaskingUtils.maskEmail(to));
        
        try {
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
                throw new EmailSendException("Mailjet API 回應錯誤: " + response.getStatus());
            }
            
            log.debug("Mailjet 郵件發送成功，狀態碼: {}", response.getStatus());
            
        } catch (MailjetException e) {
            log.error("Mailjet 郵件發送失敗: {}", e.getMessage(), e);
            throw new EmailSendException("Mailjet 郵件發送失敗", e);
        }
    }
    
    @Override
    public String getSenderType() {
        return "MAILJET";
    }
}
