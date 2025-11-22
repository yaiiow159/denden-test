package com.denden.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.RetryConfiguration;

/**
 * 重試機制配置
 *  
 * @author Member Auth System
 * @since 1.0.0
 */
@Configuration
@EnableRetry
@Slf4j
public class RetryConfig {
    
}
