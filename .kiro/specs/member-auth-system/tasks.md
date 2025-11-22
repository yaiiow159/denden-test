# Implementation Plan

- [x] 1. 專案初始化與基礎配置





  - 使用 Spring Initializr 建立 Spring Boot 3.2.x 專案（Java 17）
  - 添加必要依賴：Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Redis, Validation, Lombok
  - 配置 application.yml 基礎結構（dev, prod profiles）
  - 建立專案目錄結構（controller, service, repository, entity, dto, config, exception, util）
  - _Requirements: 7.1, 8.1_

- [x] 2. 資料庫與快取配置







  - [x] 2.1 設定 PostgreSQL 資料庫連接





    - 配置 HikariCP 連接池參數
    - 設定 JPA/Hibernate 屬性


    - _Requirements: 1.3, 2.3_
  
  - [x] 2.2 建立資料庫 Schema





    - 建立 users 資料表與索引
    - 建立 verification_tokens 資料表與索引


    - 建立 login_attempts 資料表與索引
    - 撰寫 Flyway 或 Liquibase migration scripts
    - _Requirements: 1.3, 2.3, 3.4_
  
  - [x] 2.3 設定 Redis 連接與配置





    - 配置 RedisTemplate 與 StringRedisTemplate
    - 設定序列化器（JSON）
    - 配置連接池參數
    - _Requirements: 3.5, 3.6, 4.1, 9.2_

- [x] 3. Domain Models 與 Repositories




  - [x] 3.1 實作 User Entity


    - 定義 User 類別與欄位（id, email, passwordHash, status, lastLoginAt, timestamps）
    - 添加 JPA 註解與驗證約束
    - 實作 AccountStatus enum（PENDING, ACTIVE, LOCKED）
    - _Requirements: 1.1, 1.2, 1.3, 3.2, 3.4_
  

  - [x] 3.2 實作 VerificationToken Entity

    - 定義 VerificationToken 類別與欄位
    - 建立與 User 的關聯關係
    - 實作 TokenType enum（EMAIL_VERIFICATION, PASSWORD_RESET）
    - _Requirements: 1.4, 2.1_
  
  - [x] 3.3 實作 LoginAttempt Entity


    - 定義 LoginAttempt 類別記錄登入嘗試
    - 包含 email, ipAddress, successful, attemptedAt 欄位
    - _Requirements: 3.4_

  
  - [x] 3.4 建立 Spring Data JPA Repositories

    - UserRepository：findByEmail, existsByEmail, findByStatus
    - VerificationTokenRepository：findByToken, findByUserAndType
    - LoginAttemptRepository：countByEmailAndAttemptedAtAfter
    - _Requirements: 1.2, 2.1, 3.1, 3.4_

- [x] 4. DTOs 與 Request/Response 物件







  - [ ] 4.1 建立 Request DTOs
    - RegisterRequest（email, password）with validation annotations
    - LoginRequest（email, password）
    - VerifyOtpRequest（sessionId, otp）


    - _Requirements: 1.1, 3.1, 4.1_
  
  - [ ] 4.2 建立 Response DTOs
    - AuthResponse（token, tokenType, expiresIn, user）
    - UserInfo（id, email, lastLoginAt）


    - OtpResponse（sessionId, message, expiresIn）
    - ErrorResponse（code, message, timestamp, path）
    - _Requirements: 3.6, 4.6, 5.5_
  
  - [ ] 4.3 建立 ErrorCode enum
    - 定義所有錯誤碼（1xxx-9xxx）
    - 包含錯誤碼與訊息映射
    - _Requirements: 3.2, 4.2, 9.2_

- [x] 5. 安全配置與密碼處理




  - [x] 5.1 配置 PasswordEncoder


    - 建立 BCryptPasswordEncoder bean（strength 12）
    - _Requirements: 1.3, 3.3, 9.3_


  
  - [ ] 5.2 實作密碼驗證工具
    - 建立 PasswordValidator 類別


    - 實作密碼強度檢查（長度、大小寫、數字、特殊字元）
    - _Requirements: 1.1, 9.3_
  
  - [ ] 5.3 配置 Spring Security
    - 設定 SecurityFilterChain
    - 配置公開端點（/api/v1/auth/**, /swagger-ui/**, /api-docs/**）
    - 配置受保護端點（/api/v1/users/**）
    - 停用 CSRF（使用 JWT）
    - 設定 stateless session management
    - _Requirements: 5.1, 5.2, 6.1, 6.2, 9.1_

- [x] 6. JWT Token 服務




  - [x] 6.1 實作 TokenService


    - 實作 generateJwtToken 方法（包含 userId, email claims）
    - 實作 validateToken 方法驗證簽章與有效期
    - 實作 extractEmail 方法從 token 提取 email
    - 實作 isTokenExpired 方法檢查過期
    - 從環境變數讀取 JWT secret 與 expiration
    - _Requirements: 4.4, 4.5, 5.1, 5.2_
  


  - [ ] 6.2 實作 JwtAuthenticationFilter
    - 從 Authorization header 提取 JWT token
    - 驗證 token 並設定 SecurityContext
    - 處理 token 無效或過期的情況
    - _Requirements: 5.1, 5.2, 6.1, 6.2_

- [x] 7. OTP 服務實作
  - [x] 7.1 實作 OtpService
    - 實作 generateOtp 方法產生 6 位數字 OTP
    - 實作 createOtpSession 方法儲存 OTP 至 Redis（TTL 5 分鐘）
    - 實作 validateOtp 方法驗證 OTP 正確性
    - 實作 incrementOtpAttempts 方法記錄錯誤次數
    - 實作 invalidateOtp 方法使 OTP 失效
    - _Requirements: 3.5, 3.6, 4.1, 4.2, 4.3_
  
  - [x] 7.2 實作 OTP Redis 資料結構

    - 設計 OTP session 資料格式（email, otp, attempts）
    - 設定 TTL 為 300 秒
    - _Requirements: 3.5, 4.1_

- [-] 8. Mailjet 郵件服務整合


  - [x] 8.1 添加 Mailjet 依賴與配置


    - 添加 Mailjet Java SDK 依賴
    - 配置 Mailjet API key 與 secret（從環境變數）
    - 配置寄件者 email 與名稱
    - _Requirements: 1.5, 3.6, 7.1, 7.2_

  
  - [x] 8.2 實作 EmailService





    - 實作 sendVerificationEmail 方法發送驗證連結
    - 實作 sendOtpEmail 方法發送 OTP
    - 實作 sendAccountLockedEmail 方法發送帳號鎖定通知
    - 實作錯誤處理與重試機制（最多 3 次）
    - 使用 @Async 非同步發送郵件
    - _Requirements: 1.5, 3.6, 7.2, 7.3_
  
  - [x] 8.3 設計 Email 模板





    - 建立 HTML email 模板（驗證連結、OTP、帳號鎖定）
    - 使用 Thymeleaf 或簡單的字串替換
    - _Requirements: 1.5, 3.6_
  
  - [ ]* 8.4 撰寫 EmailService 單元測試
    - 測試郵件發送成功情境
    - 測試 Mailjet API 失敗與重試機制
    - 使用 Mockito mock Mailjet client
    - _Requirements: 7.2, 7.3_

- [x] 9. 會員註冊功能實作




  - [x] 9.1 實作 AuthService.register 方法


    - 驗證 email 格式與密碼強度
    - 檢查 email 是否已存在
    - 使用 BCrypt 加密密碼
    - 建立 User entity（status = PENDING）
    - 儲存至資料庫
    - _Requirements: 1.1, 1.2, 1.3_

  
  - [x] 9.2 實作 Email 驗證 token 生成





    - 產生唯一的 verification token（UUID）
    - 建立 VerificationToken entity（TTL 24 小時）
    - 儲存至資料庫

    - _Requirements: 1.4_
  
  - [x] 9.3 整合郵件發送





    - 呼叫 EmailService 發送驗證郵件


    - 包含驗證連結（含 token）
    - _Requirements: 1.5_
  
  - [x] 9.4 實作 AuthController.register 端點





    - POST /api/v1/auth/register
    - 接收 RegisterRequest
    - 呼叫 AuthService.register
    - 回傳 201 Created 或適當錯誤
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ]* 9.5 撰寫註冊功能測試
    - 單元測試：測試各種註冊情境（成功、email 重複、密碼弱）
    - 整合測試：測試完整註冊流程
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 10. Email 帳號開通功能







  - [x] 10.1 實作 AuthService.verifyEmail 方法





    - 驗證 token 存在性與有效性
    - 檢查 token 是否已使用或過期
    - 更新 User status 為 ACTIVE
    - 標記 token 為已使用


    - _Requirements: 2.1, 2.2, 2.3_
  -



  - [x] 10.2 實作 AuthController.verifyEmail 端點





    - GET /api/v1/auth/verify-email?token={token}

    - 呼叫 AuthService.verifyEmail

    - 回傳成功訊息或錯誤
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [x] 10.3 實作重新發送驗證郵件功能





    - POST /api/v1/auth/resend-verification
    - 檢查帳號狀態（僅 PENDING 可重發）
    - 產生新的 verification token
    - 發送新的驗證郵件
    - _Requirements: 1.5, 2.1_
  
  - [ ]* 10.4 撰寫 Email 驗證測試
    - 測試驗證成功情境
    - 測試 token 過期、已使用情境
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 11. 登入第一階段 - 密碼驗證







  - [x] 11.1 實作 AuthService.login 方法




    - 驗證 email 存在性
    - 檢查帳號狀態（ACTIVE）
    - 檢查帳號是否被鎖定
    - 使用 BCrypt 驗證密碼
    - 記錄登入嘗試（成功/失敗）

    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [x] 11.2 實作帳號鎖定機制





    - 檢查最近 30 分鐘內失敗次數
    - 連續失敗 5 次後鎖定帳號 30 分鐘
    - 使用 Redis 儲存鎖定狀態

    - 發送帳號鎖定通知郵件
    - _Requirements: 3.4_
  
  - [x] 11.3 實作 OTP 生成與發送





    - 密碼驗證成功後產生 OTP
    - 建立 OTP session 並儲存至 Redis


    - 呼叫 EmailService 發送 OTP
    - 回傳 OtpResponse（含 sessionId）
    - _Requirements: 3.5, 3.6_
  
  - [x] 11.4 實作 AuthController.login 端點





    - POST /api/v1/auth/login
    - 接收 LoginRequest
    - 呼叫 AuthService.login
    - 回傳 OtpResponse 或錯誤
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  
  - [ ]* 11.5 撰寫登入第一階段測試
    - 測試密碼驗證成功與失敗
    - 測試帳號鎖定機制
    - 測試 OTP 生成與發送
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 12. 登入第二階段 - OTP 驗證







  - [x] 12.1 實作 AuthService.verifyOtp 方法




    - 從 Redis 取得 OTP session
    - 驗證 OTP 正確性與時效性
    - 檢查錯誤次數（最多 3 次）
    - OTP 驗證成功後產生 JWT token
    - 更新使用者 lastLoginAt
    - 刪除 OTP session


    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  
  - [x] 12.2 實作 AuthController.verifyOtp 端點




    - POST /api/v1/auth/verify-otp
    - 接收 VerifyOtpRequest

    - 呼叫 AuthService.verifyOtp
    - 回傳 AuthResponse（含 JWT token）或錯誤
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  
  - [x] 12.3 實作重新發送 OTP 功能




    - POST /api/v1/auth/resend-otp
    - 驗證 sessionId 有效性
    - 產生新的 OTP
    - 更新 Redis session
    - 發送新的 OTP 郵件
    - _Requirements: 4.2, 3.6_
  
  - [ ]* 12.4 撰寫 OTP 驗證測試
    - 測試 OTP 驗證成功情境
    - 測試 OTP 錯誤、過期情境
    - 測試錯誤次數限制
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 13. 使用者資訊查詢功能




  - [x] 13.1 實作 UserService


    - 實作 getCurrentUserInfo 方法取得使用者資訊
    - 實作 getLastLoginTime 方法取得最後登入時間
    - _Requirements: 5.3, 5.4, 5.5_


  
  - [x] 13.2 實作 UserController




    - GET /api/v1/users/me - 取得當前使用者資訊
    - GET /api/v1/users/me/last-login - 取得最後登入時間
    - 從 JWT token 提取使用者身份
    - 驗證使用者僅能查詢自己的資訊
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3_
  
  - [ ]* 13.3 撰寫使用者查詢測試
    - 測試已認證使用者查詢成功
    - 測試未認證使用者被拒絕
    - 測試 JWT token 過期情境
    - _Requirements: 5.1, 5.2, 6.1, 6.2_

- [x] 14. Rate Limiting 實作




  - [x] 14.1 實作 RateLimitFilter


    - 使用 Redis 實作 Token Bucket 或 Sliding Window 演算法
    - 限制每個 IP 每分鐘最多 10 次請求
    - 超過限制回傳 429 Too Many Requests
    - _Requirements: 9.2_


  
  - [x] 14.2 註冊 Filter 至 Spring Security





    - 將 RateLimitFilter 加入 filter chain


    - 設定 filter 順序
    - _Requirements: 9.2_
  
  - [ ] 14.3 撰寫 Rate Limiting 測試

    - 測試正常請求通過
    - 測試超過限制被拒絕
    - 測試限制視窗重置
    - _Requirements: 9.2_

- [x] 15. 全局異常處理




  - [x] 15.1 建立自訂異常類別


    - BusinessException（業務邏輯異常）
    - AuthenticationException（認證異常）
    - ResourceNotFoundException（資源不存在）
    - _Requirements: 3.2, 4.2, 5.2, 6.2_


  
  - [x] 15.2 實作 GlobalExceptionHandler




    - @RestControllerAdvice 處理所有異常
    - 處理 BusinessException 回傳對應錯誤碼
    - 處理 MethodArgumentNotValidException（參數驗證）
    - 處理 AccessDeniedException（權限不足）
    - 處理未預期異常回傳 500
    - 統一使用 ErrorResponse 格式
    - _Requirements: 3.2, 4.2, 5.2, 6.2_
  
  - [ ]* 15.3 撰寫異常處理測試
    - 測試各種異常回傳正確的錯誤格式
    - 測試錯誤碼映射正確
    - _Requirements: 3.2, 4.2_

- [ ] 16. 安全強化配置
  - [x] 16.1 實作 CORS 配置




    - 配置允許的來源網域
    - 設定允許的 HTTP 方法
    - 設定允許的 headers
    - 啟用 credentials
    - _Requirements: 9.4_
  
  - [ ] 16.2 實作安全標頭 Filter
    - 添加 X-Content-Type-Options: nosniff
    - 添加 X-Frame-Options: DENY
    - 添加 X-XSS-Protection: 1; mode=block
    - 添加 Strict-Transport-Security
    - 添加 Content-Security-Policy
    - _Requirements: 9.6_
  
  - [x] 16.3 實作日誌脫敏





    - 建立 LoggingFilter 記錄請求/響應
    - 遮罩敏感資訊（密碼、OTP、token）
    - 配置 Logback 日誌格式
    - _Requirements: 9.3_

- [x] 17. Swagger API 文件配置
  - [x] 17.1 添加 SpringDoc OpenAPI 依賴
    - 添加 springdoc-openapi-starter-webmvc-ui 依賴
    - _Requirements: 8.1, 8.2_
  
  - [x] 17.2 配置 OpenAPI
    - 建立 OpenApiConfig 類別
    - 設定 API 基本資訊（title, version, description）
    - 配置 JWT Bearer Authentication
    - 設定 security schemes
    - _Requirements: 8.1, 8.2_
  
  - [x] 17.3 為 Controllers 添加 API 文件註解
    - 使用 @Operation 描述每個端點
    - 使用 @ApiResponse 描述回應
    - 使用 @Parameter 描述參數
    - 使用 @Schema 描述 DTO 欄位
    - _Requirements: 8.2_
  
  - [x] 17.4 測試 Swagger UI
    - 啟動應用存取 /swagger-ui.html
    - 驗證所有端點正確顯示
    - 測試 Try it out 功能
    - 匯出 OpenAPI 3.0 JSON 規格
    - _Requirements: 8.1, 8.2, 8.3_

- [x] 18. Docker 化與部署準備




  - [x] 18.1 建立 Dockerfile


    - 使用 multi-stage build
    - 基礎映像使用 eclipse-temurin:17-jre-alpine

    - 複製 JAR 檔案
    - 設定 ENTRYPOINT
    - _Requirements: 7.1_


  
  - [x] 18.2 建立 docker-compose.yml




    - 定義 app, postgres, redis 服務
    - 配置環境變數
    - 設定 volumes 持久化資料

    - 配置網路

    - _Requirements: 7.1_
  
  - [x] 18.3 建立環境變數範本

    - 建立 .env.example 檔案

    - 列出所有必要環境變數
    - 提供範例值（非敏感資訊）
    - _Requirements: 7.1_
  
  - [ ] 18.4 撰寫部署文件
    - 建立 DEPLOYMENT.md
    - 說明本地 Docker 部署步驟
    - 說明 AWS/GCP 部署建議
    - 包含環境變數配置說明
    - _Requirements: 7.1_

- [ ] 19. 健康檢查與監控
  - [ ] 19.1 配置 Spring Actuator
    - 添加 spring-boot-starter-actuator 依賴
    - 啟用 health, info, metrics endpoints
    - 配置 management endpoints 路徑
    - _Requirements: 7.4_
  
  - [ ] 19.2 實作自訂健康檢查
    - 實作 DatabaseHealthIndicator
    - 實作 RedisHealthIndicator
    - 實作 MailjetHealthIndicator
    - _Requirements: 7.4_
  
  - [ ] 19.3 配置日誌
    - 配置 logback-spring.xml
    - 設定不同環境的日誌級別
    - 配置日誌格式（JSON for production）
    - 設定日誌檔案 rotation
    - _Requirements: 9.3_

- [ ] 20. 整合測試與驗證


  - [ ]* 20.1 撰寫端到端整合測試
    - 使用 TestContainers 啟動 PostgreSQL 與 Redis
    - 測試完整註冊流程（註冊 → 驗證 Email → 登入 → OTP → 查詢）
    - 測試錯誤情境（密碼錯誤、OTP 錯誤、帳號鎖定）
    - 測試 Rate Limiting
    - _Requirements: All_
  
  - [ ]* 20.2 撰寫安全測試
    - 測試 SQL Injection 防護
    - 測試 JWT token 篡改偵測
    - 測試未授權存取被拒絕
    - 測試 CORS 配置
    - _Requirements: 9.1, 9.4, 9.5, 9.6_
  
  - [ ] 20.3 手動測試與驗證
    - 使用 Postman 或 Swagger UI 測試所有 API
    - 驗證 Email 實際發送（使用 Mailjet）
    - 測試不同錯誤情境
    - 驗證日誌記錄正確
    - _Requirements: All_
  
  - [ ] 20.4 效能測試（可選）
    - 使用 JMeter 或 Gatling 進行負載測試
    - 測試併發登入請求
    - 驗證 Rate Limiting 效果
    - 檢查資料庫連接池狀態
    - _Requirements: 9.2_

- [x] 21. 文件完善







  - [ ] 21.1 撰寫 README.md
    - 專案簡介與功能說明
    - 技術棧列表
    - 快速開始指南
    - API 文件連結
    - 環境變數說明


    - _Requirements: All_
  
  - [ ] 21.2 撰寫 API 使用範例
    - 提供 curl 命令範例


    - 提供 Postman Collection
    - 說明完整的使用流程
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [ ] 21.3 撰寫安全最佳實踐文件
    - 說明密碼要求
    - 說明 JWT token 管理
    - 說明 Rate Limiting 限制
    - 提供安全建議
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_
