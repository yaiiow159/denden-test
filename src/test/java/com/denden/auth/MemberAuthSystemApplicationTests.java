package com.denden.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Main Application Context Test
 * 
 * 驗證 Spring Boot 應用程式上下文能夠正常載入
 */
@SpringBootTest
@ActiveProfiles("test")
class MemberAuthSystemApplicationTests {

    @Test
    void contextLoads() {
        // 驗證應用程式上下文載入成功
    }
}
