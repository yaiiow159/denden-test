package com.denden.auth.service.email;

/**
 * 郵件發送異常
 * 
 * <p>當郵件發送過程中發生錯誤時拋出此異常。
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
public class EmailSendException extends Exception {
    
    public EmailSendException(String message) {
        super(message);
    }
    
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
