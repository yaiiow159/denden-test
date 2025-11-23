package com.denden.auth.util;

/**
 * 資料遮罩工具類
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
public final class MaskingUtils {

    private MaskingUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    private static final String MASK_SYMBOL = "***";
    
    /**
     * 遮罩 Email 地址
     * 
     * <p>保留第一個字元和 @ 符號後的網域部分，中間使用 *** 遮罩</p>
     * 
     * @param email 原始 Email 地址
     * @return 遮罩後的 Email 地址
     * 
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 1) {
            return localPart + MASK_SYMBOL + domain;
        }
        
        return localPart.charAt(0) + MASK_SYMBOL + domain;
    }
    
    /**
     * 遮罩 Token 字串
     * 
     * <p>保留前 8 個字元，其餘使用 *** 遮罩</p>
     * 
     * @param token 原始 Token 字串
     * @return 遮罩後的 Token 字串
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        
        if (token.length() <= 8) {
            return MASK_SYMBOL;
        }
        
        return token.substring(0, 8) + MASK_SYMBOL;
    }
    
    /**
     * 遮罩手機號碼
     * 
     * <p>保留前 4 碼和後 3 碼，中間使用 *** 遮罩</p>
     * 
     * @param phone 原始手機號碼
     * @return 遮罩後的手機號碼
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }
        
        if (phone.length() < 7) {
            return MASK_SYMBOL;
        }
        
        String prefix = phone.substring(0, 4);
        String suffix = phone.substring(phone.length() - 3);
        
        return prefix + MASK_SYMBOL + suffix;
    }
    
    /**
     * 遮罩身分證字號
     * 
     * <p>保留第一個字元和最後一個字元，中間使用 *** 遮罩</p>
     * 
     * @param idNumber 原始身分證字號
     * @return 遮罩後的身分證字號
     */
    public static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isEmpty()) {
            return "";
        }
        
        if (idNumber.length() <= 2) {
            return MASK_SYMBOL;
        }
        
        return idNumber.charAt(0) + MASK_SYMBOL + idNumber.charAt(idNumber.length() - 1);
    }
    
    /**
     * 遮罩信用卡號
     * 
     * <p>保留後 4 碼，其餘使用 *** 遮罩</p>
     * 
     * 
     * @param cardNumber 原始信用卡號
     * @return 遮罩後的信用卡號
     * 
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "";
        }
        
        if (cardNumber.length() <= 4) {
            return MASK_SYMBOL;
        }
        
        return MASK_SYMBOL + cardNumber.substring(cardNumber.length() - 4);
    }
    
    /**
     * 通用遮罩方法
     * 
     * <p>根據指定的保留前綴和後綴長度進行遮罩</p>
     * 
     * @param value 原始值
     * @param prefixLength 保留前綴長度
     * @param suffixLength 保留後綴長度
     * @return 遮罩後的值

     */
    public static String mask(String value, int prefixLength, int suffixLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        int minLength = prefixLength + suffixLength;
        if (value.length() <= minLength) {
            return MASK_SYMBOL;
        }
        
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(value.length() - suffixLength);
        
        return prefix + MASK_SYMBOL + suffix;
    }
}
