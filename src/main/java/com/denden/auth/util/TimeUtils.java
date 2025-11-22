package com.denden.auth.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 時間工具類
 * 
 * <p>提供時間相關的轉換功能，包含：
 * <ul>
 *   <li>LocalDateTime 與 Unix timestamp (毫秒) 互轉</li>
 *   <li>時區處理</li>
 * </ul>
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@UtilityClass
public class TimeUtils {
    
    /**
     * 將 LocalDateTime 轉換為 Unix timestamp (毫秒)
     * 
     * <p>使用系統預設時區進行轉換
     * 
     * @param dateTime 要轉換的 LocalDateTime
     * @return Unix timestamp (毫秒)
     * @throws NullPointerException 當 dateTime 為 null 時
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime cannot be null");
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 將 LocalDateTime 轉換為 Unix timestamp (毫秒)，返回 double 類型
     *      
     * @param dateTime 要轉換的 LocalDateTime
     * @return Unix timestamp (毫秒) as double
     * @throws NullPointerException 當 dateTime 為 null 時
     */
    public static double toTimestampAsDouble(LocalDateTime dateTime) {
        return (double) toTimestamp(dateTime);
    }
    
    /**
     * 將 Unix timestamp (毫秒) 轉換為 LocalDateTime
     * 
     * <p>使用系統預設時區進行轉換
     * 
     * @param timestamp Unix timestamp (毫秒)
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }
    
    /**
     * 將 Unix timestamp (毫秒) 轉換為 LocalDateTime，接受 double 類型
     * 
     * @param timestamp Unix timestamp (毫秒) as double
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestamp(double timestamp) {
        return fromTimestamp((long) timestamp);
    }
    
    /**
     * 將 LocalDateTime 轉換為 Unix timestamp (秒)
     * 
     * @param dateTime 要轉換的 LocalDateTime
     * @return Unix timestamp (秒)
     * @throws NullPointerException 當 dateTime 為 null 時
     */
    public static long toTimestampSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime cannot be null");
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
    }
    
    /**
     * 將 Unix timestamp (秒) 轉換為 LocalDateTime
     * 
     * @param timestampSeconds Unix timestamp (秒)
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestampSeconds(long timestampSeconds) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestampSeconds),
                ZoneId.systemDefault()
        );
    }
}
