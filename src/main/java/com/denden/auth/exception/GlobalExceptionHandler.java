package com.denden.auth.exception;

import com.denden.auth.dto.ErrorResponse;
import com.denden.auth.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 全局異常處理器
 * 
 * <p>統一處理系統中的所有異常，並返回標準化的錯誤響應格式。
 * 
 * <p>處理的異常類型：
 * <ul>
 *   <li>{@link BusinessException} - 業務邏輯異常</li>
 *   <li>{@link com.denden.auth.exception.AuthenticationException} - 自訂認證異常</li>
 *   <li>{@link ResourceNotFoundException} - 資源不存在異常</li>
 *   <li>{@link MethodArgumentNotValidException} - 參數驗證異常</li>
 *   <li>{@link AccessDeniedException} - 權限不足異常（Spring Security）</li>
 *   <li>{@link AuthenticationException} - 認證失敗異常（Spring Security）</li>
 *   <li>{@link HttpMessageNotReadableException} - 請求體解析異常</li>
 *   <li>{@link MissingServletRequestParameterException} - 缺少請求參數異常</li>
 *   <li>{@link MethodArgumentTypeMismatchException} - 參數類型不匹配異常</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} - 不支援的 HTTP 方法異常</li>
 *   <li>{@link NoHandlerFoundException} - 找不到處理器異常</li>
 *   <li>{@link Exception} - 未預期的系統異常</li>
 * </ul>
 * 
 * <p>所有異常都會被記錄到日誌中，並返回統一的 {@link ErrorResponse} 格式。
 * 敏感資訊（如密碼、Token）不會被記錄到日誌中。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 處理業務邏輯異常
     * 
     * @param ex 業務異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, 
            HttpServletRequest request) {
        
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = mapErrorCodeToHttpStatus(errorCode);
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode.getCode(),
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("業務異常: code={}, message={}, path={}", 
                errorCode.getCode(), ex.getMessage(), request.getRequestURI());
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 處理自訂認證異常
     * 
     * @param ex 自訂認證異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(com.denden.auth.exception.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthenticationException(
            com.denden.auth.exception.AuthenticationException ex,
            HttpServletRequest request) {
        
        ErrorCode errorCode = ex.getErrorCode();
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode.getCode(),
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("認證異常: code={}, message={}, path={}, ip={}", 
                errorCode.getCode(), ex.getMessage(), request.getRequestURI(), 
                RequestUtils.getClientIp(request));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * 處理 Spring Security 認證異常
     * 
     * @param ex Spring Security 認證異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSpringSecurityAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.UNAUTHORIZED.getCode(),
            ErrorCode.UNAUTHORIZED.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("Spring Security 認證失敗: message={}, path={}, ip={}", 
                ex.getMessage(), request.getRequestURI(), RequestUtils.getClientIp(request));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * 處理資源不存在異常
     * 
     * @param ex 資源不存在異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorCode errorCode = ex.getErrorCode();
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode.getCode(),
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("資源不存在: code={}, message={}, path={}", 
                errorCode.getCode(), ex.getMessage(), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 處理參數驗證異常
     * 
     * @param ex 參數驗證異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        if (errorMessage.isEmpty()) {
            errorMessage = "參數驗證失敗";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR.getCode(),
            errorMessage,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("參數驗證失敗: message={}, path={}, ip={}", 
                errorMessage, request.getRequestURI(), RequestUtils.getClientIp(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 處理請求體解析異常
     * 
     * @param ex 請求體解析異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR.getCode(),
            "請求體格式錯誤或無法解析",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("請求體解析失敗: message={}, path={}", ex.getMessage(), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 處理缺少請求參數異常
     * 
     * @param ex 缺少請求參數異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        
        String errorMessage = String.format("缺少必要參數: %s", ex.getParameterName());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR.getCode(),
            errorMessage,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("缺少請求參數: parameter={}, path={}", ex.getParameterName(), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 處理參數類型不匹配異常
     * 
     * @param ex 參數類型不匹配異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String errorMessage = String.format("參數 %s 類型錯誤", ex.getName());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR.getCode(),
            errorMessage,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("參數類型不匹配: parameter={}, path={}", ex.getName(), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 處理不支援的 HTTP 方法異常
     * 
     * @param ex 不支援的 HTTP 方法異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        
        String errorMessage = String.format("不支援的 HTTP 方法: %s", ex.getMethod());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.UNSUPPORTED_OPERATION.getCode(),
            errorMessage,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("不支援的 HTTP 方法: method={}, path={}", ex.getMethod(), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    /**
     * 處理找不到處理器異常（404）
     * 
     * @param ex 找不到處理器異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.RESOURCE_NOT_FOUND.getCode(),
            "請求的資源不存在",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("找不到處理器: path={}, method={}", request.getRequestURI(), ex.getHttpMethod());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 處理權限不足異常
     * 
     * @param ex 權限不足異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.warn("權限不足: message={}, path={}, ip={}", 
                ex.getMessage(), request.getRequestURI(), RequestUtils.getClientIp(request));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * 處理未預期的系統異常
     * 
     * @param ex 系統異常
     * @param request HTTP 請求
     * @return 錯誤響應
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.INTERNAL_ERROR.getCode(),
            ErrorCode.INTERNAL_ERROR.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        log.error("系統異常: type={}, message={}, path={}, ip={}", 
                ex.getClass().getSimpleName(), ex.getMessage(), 
                request.getRequestURI(), RequestUtils.getClientIp(request), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    

    /**
     * 將錯誤碼映射到對應的 HTTP 狀態碼
     * 
     * @param errorCode 錯誤碼
     * @return HTTP 狀態碼
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        int code = errorCode.getCode();
        
        if (code >= 1000 && code < 2000) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        if (code >= 2000 && code < 3000) {
            if (code == 2001) {
                return HttpStatus.CONFLICT;
            }
            if (code == 2004) {
                return HttpStatus.NOT_FOUND;
            }
            return HttpStatus.BAD_REQUEST;
        }
        
        if (code >= 3000 && code < 4000) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        if (code >= 4000 && code < 5000) {
            if (code == 4001) {
                return HttpStatus.UNAUTHORIZED;
            }
            return HttpStatus.FORBIDDEN;
        }
        
        if (code >= 5000 && code < 6000) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        
        if (code >= 6000 && code < 7000) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
