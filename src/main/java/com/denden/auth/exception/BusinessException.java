package com.denden.auth.exception;

/**
 * 業務邏輯異常
 */
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * 建構業務邏輯異常
     * 
     * @param errorCode 錯誤碼
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    /**
     * 建構業務邏輯異常（自訂訊息）
     * 
     * @param errorCode 錯誤碼
     * @param message 自訂錯誤訊息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 建構業務邏輯異常（包含原因）
     * 
     * @param errorCode 錯誤碼
     * @param cause 原始異常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 建構業務邏輯異常（自訂訊息與原因）
     * 
     * @param errorCode 錯誤碼
     * @param message 自訂錯誤訊息
     * @param cause 原始異常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 取得錯誤碼
     * 
     * @return 錯誤碼
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
