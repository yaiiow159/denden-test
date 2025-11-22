package com.denden.auth.filter;

import com.denden.auth.config.SecurityProperties;
import com.denden.auth.exception.ErrorCode;
import com.denden.auth.util.RequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Rate Limiting 過濾器
 * 
 * <p>預設限制：每個 IP 每分鐘最多 10 次請求
 * <p>超過限制時回傳 429 Too Many Requests
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    private final StringRedisTemplate stringRedisTemplate;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = RequestUtils.getClientIp(request);
        String key = RATE_LIMIT_KEY_PREFIX + clientIp;
        
        try {
            String countStr = stringRedisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            int maxRequests = securityProperties.getRateLimit().getMaxRequests();
            int windowSeconds = securityProperties.getRateLimit().getWindowSeconds();
            
            if (currentCount >= maxRequests) {
                log.warn("IP 請求頻率超過限制: {}, 當前次數: {}", clientIp, currentCount);
                handleRateLimitExceeded(response);
                return;
            }
            
            Long newCount = stringRedisTemplate.opsForValue().increment(key);
            
            if (newCount != null && newCount == 1) {
                stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            
            log.debug("IP 請求頻率檢查通過: {}, 次數: {}/{}", 
                    clientIp, newCount, maxRequests);
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("IP 請求頻率檢查失敗: {}, 錯誤: {}", clientIp, e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 處理超過限流的請求
     * 
     * @param response HTTP 響應
     * @throws IOException IO 異常
     */
    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        errorResponse.put("message", ErrorCode.RATE_LIMIT_EXCEEDED.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", null);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
