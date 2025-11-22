package com.denden.auth.filter;

import com.denden.auth.util.MaskingUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日誌記錄過濾器
 * 
 * <p>記錄所有 HTTP 請求和響應，並自動遮罩敏感資訊：
 * 
 * @author Member Auth System
 * @since 1.0.0
 */
@Slf4j
@Order(2)
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final List<String> SENSITIVE_FIELDS = Arrays.asList(
            "password", "otp", "token", "secret", "authorization"
    );

    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            logRequest(requestWrapper);

            filterChain.doFilter(requestWrapper, responseWrapper);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logResponse(responseWrapper, duration);

            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * 記錄 HTTP 請求
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = getClientIp(request);

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Request: ")
                .append(method).append(" ")
                .append(uri);

        if (queryString != null) {
            logMessage.append("?").append(maskSensitiveData(queryString));
        }

        logMessage.append(" | IP: ").append(clientIp);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            logMessage.append(" | Auth: ").append(maskAuthorizationHeader(authHeader));
        }

        log.info(logMessage.toString());

        // 記錄請求 body (僅 POST/PUT/PATCH)
        if (shouldLogBody(method)) {
            String body = getRequestBody(request);
            if (body != null && !body.isEmpty()) {
                log.debug("Request Body: {}", maskSensitiveData(body));
            }
        }
    }

    /**
     * 記錄 HTTP 響應
     */
    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Response: ")
                .append("Status ").append(status)
                .append(" | Duration: ").append(duration).append("ms");

        if (status >= 400) {
            log.warn(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }

        if (log.isDebugEnabled()) {
            String body = getResponseBody(response);
            if (body != null && !body.isEmpty()) {
                log.debug("Response Body: {}", maskSensitiveData(body));
            }
        }
    }

    /**
     * 取得請求 body 內容
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }

        String body = new String(content, StandardCharsets.UTF_8);
        return truncateIfNeeded(body);
    }

    /**
     * 取得響應 body 內容
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }

        String body = new String(content, StandardCharsets.UTF_8);
        return truncateIfNeeded(body);
    }

    /**
     * 遮罩敏感資料
     * 
     * <p>使用正則表達式匹配並遮罩 JSON 中的敏感欄位</p>
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        for (String field : SENSITIVE_FIELDS) {
            Pattern pattern = Pattern.compile(
                    "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(maskedData);
            
            if (matcher.find()) {
                String maskedValue = getMaskedValue(field, matcher.group(1));
                maskedData = matcher.replaceAll("\"" + field + "\":\"" + maskedValue + "\"");
            }
        }

        for (String field : SENSITIVE_FIELDS) {
            Pattern pattern = Pattern.compile(
                    field + "=([^&]+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(maskedData);
            
            if (matcher.find()) {
                maskedData = matcher.replaceAll(field + "=***");
            }
        }

        return maskedData;
    }

    /**
     * 根據欄位類型取得遮罩值
     */
    private String getMaskedValue(String field, String value) {
        String lowerField = field.toLowerCase();
        
        if (lowerField.contains("password")) {
            return "***";
        } else if (lowerField.contains("otp")) {
            return "***";
        } else if (lowerField.contains("token") || lowerField.contains("authorization")) {
            return MaskingUtils.maskToken(value);
        } else {
            return "***";
        }
    }

    /**
     * 遮罩 Authorization header
     */
    private String maskAuthorizationHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return "";
        }

        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return "Bearer " + MaskingUtils.maskToken(token);
        }

        if (authHeader.startsWith("Basic ")) {
            return "Basic ***";
        }

        return "***";
    }

    /**
     * 取得客戶端真實 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 判斷是否應該記錄 body
     */
    private boolean shouldLogBody(String method) {
        return "POST".equalsIgnoreCase(method) 
                || "PUT".equalsIgnoreCase(method) 
                || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 截斷過長的內容
     */
    private String truncateIfNeeded(String content) {
        if (content.length() > MAX_PAYLOAD_LENGTH) {
            return content.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)";
        }
        return content;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        return path.startsWith("/swagger-ui") 
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/health");
    }
}
