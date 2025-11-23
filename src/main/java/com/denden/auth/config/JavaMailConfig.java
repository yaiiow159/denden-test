package com.denden.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.Properties;

/**
 * JavaMail 郵件服務配置
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "javamail")
@ConfigurationProperties(prefix = "spring.mail")
@Getter
@Setter
public class JavaMailConfig {
    
    private String host;
    
    private int port = 587;
    
    private String username;
    
    private String password;
    
    private String protocol = "smtp";
    
    private boolean auth = true;
    
    private boolean starttlsEnable = true;
    
    private boolean starttlsRequired = true;
    
    @Bean
    public JavaMailSender javaMailSender() {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("SMTP 主機不能為空");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("SMTP 使用者名稱不能為空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("SMTP 密碼不能為空");
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return mailSender;
    }
}
