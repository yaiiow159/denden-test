package com.denden.auth.service;

import com.denden.auth.entity.User;
import io.jsonwebtoken.Claims;

/**
 * JWT Token 服務介面
 * 
 * <p>提供 JWT Token 的生成、驗證與解析功能，用於實現無狀態的使用者認證機制。</p>
 *
 * @author Timmy
 * @since 1.0.0
 */
public interface TokenService {

    /**
     * 生成 JWT Token
     *
     * @param user 使用者實體，不可為 null
     * @return JWT Token 字串
     * @throws IllegalArgumentException 如果 user 為 null
     */
    String generateJwtToken(User user);

    /**
     * 驗證 Token 並返回 Claims
     *
     * @param token JWT Token 字串
     * @return Token 中的 Claims
     * @throws JwtException 如果 Token 無效、過期或簽章錯誤
     */
    Claims validateToken(String token);

    /**
     * 從 Token 中提取 Email
     *
     * @param token JWT Token 字串
     * @return 使用者 Email
     * @throws JwtException 如果 Token 無效或格式錯誤
     */
    String extractEmail(String token);

    /**
     * 檢查 Token 是否已過期
     *
     * @param token JWT Token 字串
     * @return 如果 Token 已過期返回 true，否則返回 false
     * @throws JwtException 如果 Token 格式錯誤
     */
    boolean isTokenExpired(String token);
}
