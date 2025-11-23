package com.denden.auth.config;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Mailjet 郵件服務配置
 * 
 */
@Configuration
@ConfigurationProperties(prefix = "app.mail.mailjet")
@Validated
@Getter
@Setter
public class MailjetConfig {

    @NotBlank(message = "Mailjet API key 不能為空")
    private String apiKey;

    @NotBlank(message = "Mailjet secret key 不能為空")
    private String secretKey;
    
    @NotBlank(message = "寄件者 Email 不能為空")
    @Email(message = "寄件者 Email 格式不正確")
    private String fromEmail;
    
    @NotBlank(message = "寄件者名稱不能為空")
    private String fromName;
    
    @Bean
    @ConditionalOnProperty(
        name = "app.mail.provider", 
        havingValue = "mailjet", 
        matchIfMissing = true
    )
    public MailjetClient mailjetClient() {
        ClientOptions options = ClientOptions.builder()
                .apiKey(apiKey)
                .apiSecretKey(secretKey)
                .build();
        
        return new MailjetClient(options);
    }
}
