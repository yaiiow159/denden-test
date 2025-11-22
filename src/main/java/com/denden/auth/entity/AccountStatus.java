package com.denden.auth.entity;

/**
 * 帳號狀態列舉
 * <p>
 * 定義會員帳號的三種狀態：
 * <ul>
 *   <li>PENDING - 待驗證：帳號已註冊但尚未完成 Email 驗證</li>
 *   <li>ACTIVE - 已啟用：帳號已完成驗證，可正常使用</li>
 *   <li>LOCKED - 已鎖定：因安全原因（如多次登入失敗）暫時鎖定</li>
 * </ul>
 *
 * @author Member Auth System
 * @since 1.0.0
 */
public enum AccountStatus {
    /**
     * 待驗證狀態
     * 帳號已註冊但尚未完成 Email 驗證
     */
    PENDING,

    /**
     * 已啟用狀態
     * 帳號已完成驗證，可正常使用系統功能
     */
    ACTIVE,

    /**
     * 已鎖定狀態
     * 因安全原因（如連續登入失敗）暫時鎖定，需等待解鎖時間
     */
    LOCKED
}
