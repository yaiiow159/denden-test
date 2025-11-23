package com.denden.auth.config;

import com.denden.auth.filter.JwtAuthenticationFilter;
import com.denden.auth.filter.LoggingFilter;
import com.denden.auth.filter.RateLimitFilter;
import com.denden.auth.service.TokenService;
import com.denden.auth.service.email.EmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class TestSecurityConfig {

    @MockBean
    private TokenService tokenService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LoggingFilter loggingFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;
    
    @MockBean
    private EmailSender emailSender;
    
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
}
