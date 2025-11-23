package com.denden.auth.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 郵件發送器工廠
 * 
 * 根據配置自動注入可用的發送器，並提供獲取當前發送器的方法。
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Component
@Slf4j
public class EmailSenderFactory {
    
    private final Map<String, EmailSender> senderMap;
    private final EmailSender activeSender;
    
    /**
     * 構造函數，自動注入所有可用的 EmailSender 實現
     * 
     * @param senders Spring 容器中所有的 EmailSender 實現
     */
    @Autowired
    public EmailSenderFactory(List<EmailSender> senders) {
        this.senderMap = senders.stream()
            .collect(Collectors.toMap(
                EmailSender::getSenderType,
                Function.identity()
            ));
        
        this.activeSender = senders.isEmpty() ? null : senders.get(0);
        
        if (activeSender != null) {
            log.info("郵件發送器初始化完成，當前使用: {}", activeSender.getSenderType());
            log.info("可用的發送器: {}", senderMap.keySet());
        } else {
            log.warn("未找到可用的郵件發送器實現");
        }
    }
    
    /**
     * 獲取當前的郵件發送器
     * 
     * @return 當前配置的郵件發送器
     * @throws IllegalStateException 當沒有可用的發送器時
     */
    public EmailSender getActiveSender() {
        if (activeSender == null) {
            throw new IllegalStateException("未配置可用的郵件發送器");
        }
        return activeSender;
    }
    
    /**
     * 根據類型獲取特定的郵件發送器
     * 
     * @param type 發送器類型（MAILJET, JAVAMAIL 等）
     * @return 對應的郵件發送器，如果不存在則返回 null
     */
    public EmailSender getSender(String type) {
        return senderMap.get(type);
    }
}
