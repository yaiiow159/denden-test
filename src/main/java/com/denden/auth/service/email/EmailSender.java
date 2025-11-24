package com.denden.auth.service.email;

/**
 * 郵件發送策略介面
 *  
 * @author Timmy
 * @since 1.0.0
 */
public interface EmailSender {
    
    /**
     * 發送郵件
     * 
     * @param to 收件者 Email 地址
     * @param subject 郵件主旨
     * @param htmlContent 郵件 HTML 內容
     * @throws EmailSendException 當郵件發送失敗時拋出
     */
    void send(String to, String subject, String htmlContent) throws EmailSendException;
    
    /**
     * 獲取發送器類型名稱
     * 
     * @return 發送器類型（如：MAILJET, JAVAMAIL）
     */
    String getSenderType();
}
