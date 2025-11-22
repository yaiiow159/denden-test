package com.denden.auth.config;

import com.denden.auth.filter.JwtAuthenticationFilter;
import com.denden.auth.filter.LoggingFilter;
import com.denden.auth.filter.RateLimitFilter;
import com.denden.auth.service.TokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
}
