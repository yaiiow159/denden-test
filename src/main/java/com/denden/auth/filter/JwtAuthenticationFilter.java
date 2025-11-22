package com.denden.auth.filter;

import com.denden.auth.service.TokenService;
import com.denden.auth.util.MaskingUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 認證過濾器
 * 從 Authorization Header 提取並驗證 JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && StringUtils.hasText(jwt)) {
                authenticateUser(jwt, request);
            } else {
                log.debug("請求中未找到 JWT token: {}", request.getRequestURI());
            }

        } catch (JwtException e) {
            log.warn("JWT 驗證失敗，請求: {}，錯誤: {}", 
                request.getRequestURI(), e.getMessage());
        } catch (Exception e) {
            log.error("處理 JWT 認證時發生錯誤，請求: {}，錯誤: {}", 
                request.getRequestURI(), e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX_LENGTH);
            log.debug("從 Authorization header 提取 JWT token: {}...", 
                token.substring(0, Math.min(8, token.length())));
            return token;
        }

        return null;
    }

    private void authenticateUser(String jwt, HttpServletRequest request) {
        String email = tokenService.extractEmail(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = User.builder()
                    .username(email)
                    .password("")
                    .authorities(Collections.emptyList())
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("使用者認證成功: {}", MaskingUtils.maskEmail(email));
        }
    }

}
