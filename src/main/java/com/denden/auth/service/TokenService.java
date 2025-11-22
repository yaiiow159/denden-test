package com.denden.auth.service;

import com.denden.auth.entity.User;
import io.jsonwebtoken.Claims;

/**
 * JWT Token 服務介面
 * <p>
 * 提供 JWT Token 的生成、驗證與解析功能，用於實現無狀態的使用者認證機制。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>生成包含使用者資訊的 JWT Token</li>
 *   <li>驗證 Token 的簽章與有效期</li>
 *   <li>從 Token 中提取使用者資訊</li>
 *   <li>檢查 Token 是否過期</li>
 * </ul>
 *
 * @author Member Auth System
 * @since 1.0.0
 */
public interface TokenService {

    /**
     * 生成 JWT Token
     * <p>
     * 根據使用者資訊生成包含 userId 和 email 的 JWT Token。
     * </p>
     *
     * @param user 使用者實體，不可為 null
     * @return JWT Token 字串
     * @throws IllegalArgumentException 如果 user 為 null
     */
    String generateJwtToken(User user);

    /**
     * 驗證 Token 並返回 Claims
     * <p>
     * 驗證 Token 的簽章與有效期，如果驗證通過則返回 Token 中的所有 Claims。
     * </p>
     *
     * @param token JWT Token 字串
     * @return Token 中的 Claims
     * @throws JwtException 如果 Token 無效、過期或簽章錯誤
     */
    Claims validateToken(String token);

    /**
     * 從 Token 中提取 Email
     * <p>
     * 從 JWT Token 的 subject (sub) claim 中提取使用者 Email。
     * </p>
     *
     * @param token JWT Token 字串
     * @return 使用者 Email
     * @throws JwtException 如果 Token 無效或格式錯誤
     */
    String extractEmail(String token);

    /**
     * 檢查 Token 是否已過期
     * <p>
     * 比較 Token 的過期時間與當前時間，判斷 Token 是否仍然有效。
     * </p>
     *
     * @param token JWT Token 字串
     * @return 如果 Token 已過期返回 true，否則返回 false
     * @throws JwtException 如果 Token 格式錯誤
     */
    boolean isTokenExpired(String token);
}
