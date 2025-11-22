package com.denden.auth.service.impl;

import com.denden.auth.entity.User;
import com.denden.auth.service.TokenService;
import com.denden.auth.util.MaskingUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 服務實作
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.issuer}")
    private String jwtIssuer;

    @Override
    public String generateJwtToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());

        log.debug("產生 JWT token，使用者: {} (ID: {})", 
            MaskingUtils.maskEmail(user.getEmail()), user.getId());

        String token = Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtIssuer)
                .signWith(getSigningKey())
                .compact();

        log.info("成功產生 JWT token，使用者 ID: {}", user.getId());
        return token;
    }

    @Override
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Token 驗證成功，使用者: {}", 
                MaskingUtils.maskEmail(claims.getSubject()));
            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已過期: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("Token 簽章無效: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.error("Token 驗證失敗: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String extractEmail(String token) {
        Claims claims = validateToken(token);
        String email = claims.getSubject();
        log.debug("從 Token 提取 Email: {}", MaskingUtils.maskEmail(email));
        return email;
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            
            if (expired) {
                log.debug("Token 已過期，過期時間: {}", expiration);
            }
            
            return expired;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已過期: {}", e.getMessage());
            return true;
        } catch (JwtException e) {
            log.error("檢查 Token 過期時發生錯誤: {}", e.getMessage());
            throw e;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
