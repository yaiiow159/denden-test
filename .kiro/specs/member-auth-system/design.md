# Design Document

## Overview

會員認證系統採用 Spring Boot 3.x 框架開發，實現安全的會員註冊、雙因素認證登入與帳號查詢功能。系統採用分層架構設計，整合 Mailjet API 進行郵件發送，使用 JWT 進行無狀態認證，並實施多層安全防護機制。

### 技術棧選擇

- **框架**: Spring Boot 3.2.x (Java 17)
- **安全**: Spring Security 6.x + JWT
- **資料庫**: PostgreSQL 15+ (推薦) 或 MySQL 8+
- **快取**: Redis 7+ (用於 OTP、Token 黑名單、Rate Limiting)
- **郵件服務**: Mailjet API
- **API 文件**: SpringDoc OpenAPI 3 (Swagger)
- **建構工具**: Maven
- **部署**: Docker + Docker Compose (可部署至 AWS/GCP)

### 系統特性

- RESTful API 設計
- 無狀態認證 (JWT)
- 非同步郵件發送
- 分散式快取支援
- 完整的錯誤處理與日誌記錄
- API Rate Limiting
- 安全標頭與 CORS 配置

## Architecture

### 系統架構圖

```
┌─────────────┐
│   Client    │
│ (Web/Mobile)│
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────────────────────────────┐
│         API Gateway Layer               │
│  - Rate Limiting                        │
│  - CORS Filter                          │
│  - Security Headers                     │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│      Controller Layer                   │
│  - AuthController                       │
│  - UserController                       │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│       Service Layer                     │
│  - AuthService                          │
│  - UserService                          │
│  - EmailService                         │
│  - TokenService                         │
│  - OtpService                           │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│     Repository Layer                    │
│  - UserRepository                       │
│  - VerificationTokenRepository          │
│  - LoginAttemptRepository               │
└──────┬──────────────────────────────────┘
       │
       ▼
┌──────────────┬──────────────┬───────────┐
│  PostgreSQL  │    Redis     │  Mailjet  │
│   Database   │    Cache     │    API    │
└──────────────┴──────────────┴───────────┘
```

### 分層架構說明

#### 1. API Gateway Layer (過濾器層)
- **RateLimitFilter**: 實施 IP 級別的請求限流
- **SecurityHeaderFilter**: 添加安全響應標頭
- **CorsFilter**: 處理跨域請求

#### 2. Controller Layer (控制器層)
- **AuthController**: 處理註冊、登入、OTP 驗證、Email 驗證
- **UserController**: 處理使用者資訊查詢

#### 3. Service Layer (業務邏輯層)
- **AuthService**: 核心認證邏輯
- **UserService**: 使用者管理邏輯
- **EmailService**: 郵件發送邏輯（整合 Mailjet）
- **TokenService**: JWT Token 生成與驗證
- **OtpService**: OTP 生成、驗證與快取管理

#### 4. Repository Layer (資料存取層)
- Spring Data JPA repositories
- 資料庫操作抽象

#### 5. External Services
- **PostgreSQL**: 持久化資料存儲
- **Redis**: 快取 OTP、Rate Limiting 計數、Token 黑名單
- **Mailjet**: 第三方郵件服務

## Components and Interfaces

### 1. Domain Models

#### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status; // PENDING, ACTIVE, LOCKED
    
    private LocalDateTime lastLoginAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
```

#### VerificationToken Entity
```java
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String token;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private TokenType type; // EMAIL_VERIFICATION, PASSWORD_RESET
    
    private LocalDateTime expiresAt;
    
    private boolean used;
    
    private LocalDateTime createdAt;
}
```

#### LoginAttempt Entity
```java
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    
    private String ipAddress;
    
    private boolean successful;
    
    private LocalDateTime attemptedAt;
}
```

### 2. DTOs (Data Transfer Objects)

#### Request DTOs
```java
public record RegisterRequest(
    @Email String email,
    @Size(min = 8, max = 100) String password
) {}

public record LoginRequest(
    @Email String email,
    @NotBlank String password
) {}

public record VerifyOtpRequest(
    @NotBlank String sessionId,
    @Pattern(regexp = "\\d{6}") String otp
) {}
```

#### Response DTOs
```java
public record AuthResponse(
    String token,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {}

public record UserInfo(
    Long id,
    String email,
    LocalDateTime lastLoginAt
) {}

public record OtpResponse(
    String sessionId,
    String message,
    Long expiresIn
) {}
```

### 3. Service Interfaces

#### AuthService
```java
public interface AuthService {
    void register(RegisterRequest request);
    OtpResponse login(LoginRequest request);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void resendOtp(String sessionId);
}
```

#### UserService
```java
public interface UserService {
    UserInfo getCurrentUserInfo(String email);
    LocalDateTime getLastLoginTime(String email);
    void updateLastLoginTime(String email);
    boolean isAccountLocked(String email);
    void lockAccount(String email, Duration duration);
}
```

#### EmailService
```java
public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendOtpEmail(String to, String otp);
    void sendAccountLockedEmail(String to);
}
```

#### TokenService
```java
public interface TokenService {
    String generateJwtToken(User user);
    Claims validateToken(String token);
    String extractEmail(String token);
    boolean isTokenExpired(String token);
}
```

#### OtpService
```java
public interface OtpService {
    String generateOtp();
    String createOtpSession(String email, String otp);
    boolean validateOtp(String sessionId, String otp);
    void invalidateOtp(String sessionId);
    int incrementOtpAttempts(String sessionId);
}
```

### 4. API Endpoints

#### AuthController
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/verify-otp
GET    /api/v1/auth/verify-email?token={token}
POST   /api/v1/auth/resend-verification
POST   /api/v1/auth/resend-otp
```

#### UserController
```
GET    /api/v1/users/me
GET    /api/v1/users/me/last-login
```

## Data Models

### Database Schema

#### users table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
```

#### verification_tokens table
```sql
CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tokens_token ON verification_tokens(token);
CREATE INDEX idx_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_tokens_expires_at ON verification_tokens(expires_at);
```

#### login_attempts table
```sql
CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    successful BOOLEAN NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempts_email ON login_attempts(email);
CREATE INDEX idx_attempts_attempted_at ON login_attempts(attempted_at);
```

### Redis Data Structures

#### OTP Storage
```
Key: otp:session:{sessionId}
Value: {
    "email": "user@example.com",
    "otp": "123456",
    "attempts": 0
}
TTL: 300 seconds (5 minutes)
```

#### Rate Limiting
```
Key: rate_limit:{ip}
Value: request_count
TTL: 60 seconds
```

#### Account Lock
```
Key: account_lock:{email}
Value: locked_until_timestamp
TTL: 1800 seconds (30 minutes)
```

## Error Handling

### 錯誤碼設計

```java
public enum ErrorCode {
    // Authentication Errors (1xxx)
    INVALID_CREDENTIALS(1001, "Invalid email or password"),
    ACCOUNT_NOT_ACTIVATED(1002, "Account not activated"),
    ACCOUNT_LOCKED(1003, "Account temporarily locked"),
    INVALID_OTP(1004, "Invalid or expired OTP"),
    OTP_ATTEMPTS_EXCEEDED(1005, "Too many OTP attempts"),
    
    // Registration Errors (2xxx)
    EMAIL_ALREADY_EXISTS(2001, "Email already registered"),
    INVALID_EMAIL_FORMAT(2002, "Invalid email format"),
    WEAK_PASSWORD(2003, "Password does not meet requirements"),
    
    // Token Errors (3xxx)
    INVALID_TOKEN(3001, "Invalid or expired token"),
    TOKEN_ALREADY_USED(3002, "Token already used"),
    
    // Authorization Errors (4xxx)
    UNAUTHORIZED(4001, "Authentication required"),
    FORBIDDEN(4002, "Access denied"),
    
    // Rate Limiting (5xxx)
    RATE_LIMIT_EXCEEDED(5001, "Too many requests"),
    
    // External Service Errors (6xxx)
    EMAIL_SERVICE_ERROR(6001, "Failed to send email"),
    
    // System Errors (9xxx)
    INTERNAL_ERROR(9001, "Internal server error");
}
```

### 統一錯誤響應格式

```java
public record ErrorResponse(
    int code,
    String message,
    LocalDateTime timestamp,
    String path
) {}
```

### 全局異常處理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
        BusinessException ex, HttpServletRequest request) {
        // 處理業務邏輯異常
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 處理參數驗證異常
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex, HttpServletRequest request) {
        // 處理未預期異常
    }
}
```

## Security Implementation

### 1. 密碼安全

#### 密碼強度要求
- 最少 8 字元
- 至少包含一個大寫字母
- 至少包含一個小寫字母
- 至少包含一個數字
- 至少包含一個特殊字元

#### 密碼加密
```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 強度 12
    }
}
```

### 2. JWT 配置

```java
public class JwtConfig {
    private String secret; // 從環境變數讀取
    private long expirationMs = 86400000; // 24 hours
    private String issuer = "member-auth-system";
    
    // JWT Claims 結構
    // {
    //   "sub": "user@example.com",
    //   "userId": 123,
    //   "iat": 1234567890,
    //   "exp": 1234654290,
    //   "iss": "member-auth-system"
    // }
}
```

### 3. Rate Limiting 策略

```java
@Component
public class RateLimitFilter implements Filter {
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    // 使用 Redis 實現分散式 Rate Limiting
    // 使用 Token Bucket 或 Sliding Window 演算法
}
```

### 4. CORS 配置

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://yourdomain.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 5. 安全標頭

```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        // X-Content-Type-Options: nosniff
        // X-Frame-Options: DENY
        // X-XSS-Protection: 1; mode=block
        // Strict-Transport-Security: max-age=31536000; includeSubDomains
        // Content-Security-Policy: default-src 'self'
    }
}
```

### 6. 防止帳號列舉攻擊

- 登入失敗時使用通用錯誤訊息
- 註冊時不透露 Email 是否已存在（發送通知郵件）
- 統一響應時間避免時序攻擊

### 7. SQL Injection 防護

- 使用 Spring Data JPA 參數化查詢
- 避免動態 SQL 拼接
- 啟用 Hibernate SQL 日誌審查

## Testing Strategy

### 1. 單元測試 (Unit Tests)

#### 測試範圍
- Service 層業務邏輯
- Utility 類別（OTP 生成、Token 驗證）
- 密碼驗證邏輯
- 錯誤處理邏輯

#### 測試工具
- JUnit 5
- Mockito
- AssertJ

#### 測試範例
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AuthServiceImpl authService;
    
    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com", "SecurePass123!");
        
        // When & Then
        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(anyString()))
            .thenReturn(true);
        
        // When & Then
        assertThrows(BusinessException.class, 
            () -> authService.register(request));
    }
}
```

### 2. 整合測試 (Integration Tests)

#### 測試範圍
- API 端點完整流程
- 資料庫操作
- Redis 快取操作
- 外部服務整合（使用 Mock Server）

#### 測試工具
- Spring Boot Test
- TestContainers (PostgreSQL, Redis)
- WireMock (模擬 Mailjet API)
- RestAssured

#### 測試範例
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static GenericContainer<?> redis = 
        new GenericContainer<>("redis:7").withExposedPorts(6379);
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void completeRegistrationFlow_ShouldSucceed() {
        // 1. Register
        RegisterRequest request = new RegisterRequest(
            "test@example.com", "SecurePass123!");
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/v1/auth/register", request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // 2. Verify email (extract token from mock email)
        // 3. Login
        // 4. Verify OTP
        // 5. Access protected resource
    }
}
```

### 3. 安全測試

#### 測試項目
- SQL Injection 測試
- XSS 測試
- CSRF 測試
- Rate Limiting 測試
- JWT Token 篡改測試
- 密碼強度測試

### 4. 效能測試

#### 測試工具
- JMeter 或 Gatling

#### 測試場景
- 併發登入請求
- 大量註冊請求
- Rate Limiting 驗證

## Deployment Architecture

### Docker Compose 部署

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=member_auth
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### 環境變數配置

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=member_auth
DB_USERNAME=admin
DB_PASSWORD=${DB_PASSWORD}

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

# JWT
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION_MS=86400000

# Mailjet
MAILJET_API_KEY=${MAILJET_API_KEY}
MAILJET_SECRET_KEY=${MAILJET_SECRET_KEY}
MAILJET_FROM_EMAIL=noreply@yourdomain.com
MAILJET_FROM_NAME=Member Auth System

# Application
APP_BASE_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### AWS 部署建議

#### 架構
- **EC2**: 應用伺服器（使用 t3.micro 免費方案）
- **RDS**: PostgreSQL 資料庫（使用 db.t3.micro 免費方案）
- **ElastiCache**: Redis（使用 cache.t3.micro）
- **Application Load Balancer**: HTTPS 終止與負載均衡
- **Route 53**: DNS 管理
- **ACM**: SSL/TLS 憑證

#### 成本優化
- 使用 AWS Free Tier
- 單一 EC2 實例（可後續擴展）
- RDS 單一 AZ 部署
- 使用 CloudWatch 免費監控額度

### GCP 部署建議

#### 架構
- **Cloud Run**: 容器化應用部署（按需計費）
- **Cloud SQL**: PostgreSQL 資料庫
- **Memorystore**: Redis
- **Cloud Load Balancing**: HTTPS 負載均衡
- **Cloud DNS**: DNS 管理

#### 成本優化
- Cloud Run 自動擴縮容
- Cloud SQL 使用 db-f1-micro
- 使用 GCP Free Tier

## Monitoring and Logging

### 日誌策略

#### 日誌級別
- **ERROR**: 系統錯誤、外部服務失敗
- **WARN**: 登入失敗、OTP 錯誤、Rate Limiting 觸發
- **INFO**: 成功的業務操作（註冊、登入、Email 發送）
- **DEBUG**: 詳細的執行流程（開發環境）

#### 日誌格式
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "level": "INFO",
  "logger": "com.example.auth.service.AuthService",
  "message": "User registered successfully",
  "userId": 123,
  "email": "u***@example.com",
  "ipAddress": "192.168.1.1",
  "traceId": "abc123"
}
```

#### 敏感資訊遮罩
- Email: 顯示前 1 字元與網域
- 密碼: 完全不記錄
- OTP: 完全不記錄
- Token: 僅記錄前 8 字元

### 監控指標

#### 應用指標
- API 請求數與響應時間
- 錯誤率
- 登入成功/失敗率
- OTP 驗證成功/失敗率
- Email 發送成功/失敗率

#### 系統指標
- CPU 使用率
- 記憶體使用率
- 資料庫連接池狀態
- Redis 連接狀態

#### 業務指標
- 每日註冊數
- 每日活躍使用者數
- 平均登入時間
- 帳號鎖定次數

### 健康檢查

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @GetMapping("/health")
    public HealthStatus health() {
        return HealthStatus.builder()
            .status("UP")
            .database(checkDatabase())
            .redis(checkRedis())
            .mailjet(checkMailjet())
            .build();
    }
}
```

## Configuration Management

### application.yml 結構

```yaml
spring:
  application:
    name: member-auth-system
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
  
  mail:
    mailjet:
      api-key: ${MAILJET_API_KEY}
      secret-key: ${MAILJET_SECRET_KEY}
      from-email: ${MAILJET_FROM_EMAIL}
      from-name: ${MAILJET_FROM_NAME}

app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration-ms: ${JWT_EXPIRATION_MS}
    rate-limit:
      max-requests: 10
      window-seconds: 60
    otp:
      length: 6
      expiration-seconds: 300
      max-attempts: 3
    verification-token:
      expiration-hours: 24
    account-lock:
      max-failed-attempts: 5
      lock-duration-minutes: 30
  
  base-url: ${APP_BASE_URL}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

## API Documentation

### Swagger 配置

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Member Authentication System API")
                .version("1.0.0")
                .description("安全的會員註冊、登入與查詢系統")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com")))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### API 文件範例

#### POST /api/v1/auth/register
```yaml
summary: 註冊新會員
requestBody:
  required: true
  content:
    application/json:
      schema:
        type: object
        properties:
          email:
            type: string
            format: email
          password:
            type: string
            minLength: 8
responses:
  201:
    description: 註冊成功，驗證郵件已發送
  400:
    description: 請求參數錯誤
  409:
    description: Email 已被註冊
  429:
    description: 請求過於頻繁
```

## Implementation Phases

### Phase 1: 基礎架構搭建
- 專案初始化與依賴配置
- 資料庫 Schema 建立
- Redis 配置
- 基礎 Entity 與 Repository

### Phase 2: 核心認證功能
- 註冊功能實現
- Email 驗證功能
- 密碼加密與驗證
- JWT Token 生成與驗證

### Phase 3: 雙因素認證
- OTP 生成與驗證
- 登入流程實現
- 帳號鎖定機制

### Phase 4: 郵件服務整合
- Mailjet API 整合
- 郵件模板設計
- 非同步發送機制
- 錯誤處理與重試

### Phase 5: 安全強化
- Rate Limiting 實現
- CORS 配置
- 安全標頭配置
- 日誌脫敏

### Phase 6: API 文件與測試
- Swagger 配置
- 單元測試
- 整合測試
- API 文件完善

### Phase 7: 部署準備
- Docker 化
- 環境變數配置
- 健康檢查
- 監控配置

## Design Decisions and Rationales

### 1. 為什麼選擇 PostgreSQL 而非 MySQL？
- 更好的 JSON 支援（未來擴展）
- 更嚴格的資料完整性
- 更好的併發處理能力
- 開源且功能完整

### 2. 為什麼使用 Redis 而非記憶體快取？
- 分散式部署支援
- 持久化選項
- 豐富的資料結構
- 支援 TTL 自動過期

### 3. 為什麼選擇 JWT 而非 Session？
- 無狀態設計，易於水平擴展
- 減少伺服器記憶體壓力
- 支援跨域認證
- 適合微服務架構

### 4. 為什麼實施 Rate Limiting？
- 防止暴力破解攻擊
- 保護系統資源
- 防止 DDoS 攻擊
- 符合安全最佳實踐

### 5. 為什麼使用 BCrypt 而非其他加密演算法？
- 專為密碼設計
- 自動加鹽
- 可調整計算成本
- 業界標準

### 6. 為什麼需要帳號鎖定機制？
- 防止暴力破解
- 保護使用者帳號安全
- 符合安全合規要求

### 7. 為什麼 OTP 只有 5 分鐘有效期？
- 平衡安全性與使用者體驗
- 減少被攔截後的風險
- 符合業界標準（通常 3-10 分鐘）

## Future Enhancements

### 短期優化
- 支援 Google Authenticator (TOTP)
- 記住裝置功能
- 密碼重設功能
- 社交登入整合（Google, Facebook）

### 中期優化
- 多語言支援
- 使用者個人資料管理
- 登入歷史記錄查詢
- 異常登入通知

### 長期優化
- 生物識別認證
- 風險評分系統
- 機器學習異常檢測
- 多租戶支援
