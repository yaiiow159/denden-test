# Member Authentication System (會員認證系統)



企業級會員認證系統，提供完整的註冊、雙因素認證登入與帳號管理功能。

##  功能特性

### 核心功能
-  **會員註冊與 Email 驗證** - 支援郵件驗證機制
-  **雙因素認證登入** - 密碼 + Email OTP 雙重保護
-  **JWT Token 認證** - 無狀態的安全認證機制
-  **動態郵件渠道切換** - 支援 Mailjet / JavaMail，可動態切換
-  **帳號安全機制** - 登入失敗鎖定、密碼強度驗證
- **登入歷史追蹤** - 記錄登入時間、IP、裝置資訊

### 系統特性
-  **自動資料清理** - 定時清理過期的 Token 和登入記錄
-  **重試機制** - 郵件發送失敗自動重試
-  **完整日誌** - 結構化 JSON 日誌
-  **容器化部署** - Docker + Docker Compose
-  **CI/CD 自動化** - GitHub Actions 自動測試與部署
-  **API 文件** - Swagger UI 互動式文件

## 🛠 技術棧

### 後端框架
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Security**: Spring Security 6.x + JWT
- **ORM**: Spring Data JPA + Hibernate
- **Migration**: Flyway

### 資料存儲
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Connection Pool**: HikariCP

### 郵件服務
- **Primary**: Mailjet API
- **Fallback**: JavaMail (SMTP)

### 開發工具
- **Build**: Maven 3.8+
- **Testing**: JUnit 5 + Mockito
- **API Docs**: SpringDoc OpenAPI 3
- **Logging**: Logback + JSON Encoder

### DevOps
- **Container**: Docker + Docker Compose
- **CI/CD**: GitHub Actions
- **Registry**: GitHub Container Registry (GHCR)

## 前置需求

### 開發環境
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Git

### 生產環境
- Docker & Docker Compose
- 至少 2GB RAM
- PostgreSQL 15+ (或使用 Docker)
- Redis 7+ (或使用 Docker)

### 第三方服務
- **郵件服務**（擇一）：
  - Mailjet 帳號（推薦，免費 6,000 封/月）
  - Gmail 應用程式密碼
  - 其他 SMTP 服務

##  快速開始

### 1. Clone 專案

```bash
git clone https://github.com/yaiiow159/member-auth-system.git
cd member-auth-system
```

### 2. 配置環境變數

```bash
# 複製環境變數範本
cp .env.example .env

# 編輯 .env 文件
nano .env
```

必要配置：
```bash
# JWT 密鑰（至少 256 位元）
JWT_SECRET=your-secret-key-min-256-bits

# 郵件服務（Mailjet）
MAIL_PROVIDER=mailjet
MAILJET_API_KEY=your_api_key
MAILJET_SECRET_KEY=your_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com

# 應用程式 URL
APP_BASE_URL=http://localhost:8080
```

### 3. 啟動服務

```bash
# 使用 Docker Compose 啟動所有服務
docker-compose up -d

# 查看日誌
docker-compose logs -f app
```

### 4. 訪問應用

- **API 文件**: http://localhost:8080/swagger-ui.html
- **健康檢查**: http://localhost:8080/actuator/health
- **API 端點**: http://localhost:8080/api/v1

##  API 端點

### 認證 API

| 方法 | 端點 | 說明 | 認證 |
|------|------|------|------|
| POST | `/api/v1/auth/register` | 註冊新帳號 | ❌ |
| GET | `/api/v1/auth/verify-email` | 驗證 Email | ❌ |
| POST | `/api/v1/auth/login` | 登入（第一階段） | ❌ |
| POST | `/api/v1/auth/verify-otp` | 驗證 OTP（第二階段） | ❌ |

### 使用者 API

| 方法 | 端點 | 說明 | 認證 |
|------|------|------|------|
| GET | `/api/v1/users/me` | 取得使用者資訊 | ✅ |
| GET | `/api/v1/users/me/last-login` | 取得最後登入時間 | ✅ |
| GET | `/api/v1/users/me/login-history` | 取得登入歷史 | ✅ |

### 範例請求

**註冊**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@1234"
  }'
```

**登入**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234"
  }'
```

## 測試

### 運行所有測試

```bash
mvn clean test
```

### 運行特定測試

```bash
# 測試認證功能
mvn test -Dtest=AuthControllerTest

# 測試使用者功能
mvn test -Dtest=UserControllerTest
```

### 測試覆蓋率

```bash
mvn clean test jacoco:report
```

報告位置：`target/site/jacoco/index.html`

## 部署

### 自動部署（推薦）

專案已配置 GitHub Actions CI/CD，推送到 `main` 分支會自動：

1. ✅ 運行測試
2. ✅ 構建 Docker 映像
3. ✅ 推送到 GHCR
4. ✅ 部署到伺服器
5. ✅ 執行健康檢查

#### 設定 GitHub Secrets

在 GitHub Repository Settings > Secrets 中添加：

| Secret | 說明 |
|--------|------|
| `SERVER_HOST` | 伺服器 IP 或域名 |
| `SERVER_USER` | SSH 使用者名稱 |
| `SERVER_SSH_KEY` | SSH 私鑰 |
| `SERVER_PORT` | SSH 端口（預設 22） |
| `GHCR_TOKEN` | GitHub Personal Access Token |

#### 伺服器準備

```bash
# 1. SSH 登入伺服器
ssh user@your-server

# 2. 執行初始化腳本
curl -fsSL https://raw.githubusercontent.com/yaiiow159/member-auth-system/main/scripts/prepare-server.sh | bash

# 3. 配置環境變數
nano /opt/member-auth/.env
```

### 手動部署

詳細步驟請參考 [DEPLOYMENT.md](DEPLOYMENT.md)

## 專案結構

```
member-auth-system/
├── .github/
│   └── workflows/
│       └── deploy.yml          # CI/CD 配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/denden/auth/
│   │   │       ├── config/     # 配置類
│   │   │       ├── controller/ # REST 控制器
│   │   │       ├── entity/     # JPA 實體
│   │   │       ├── repository/ # 資料存取層
│   │   │       ├── service/    # 業務邏輯層
│   │   │       ├── filter/     # 過濾器
│   │   │       ├── util/       # 工具類
│   │   │       └── exception/  # 異常處理
│   │   └── resources/
│   │       ├── application.yml # 應用配置
│   │       ├── logback-spring.xml # 日誌配置
│   │       ├── db/migration/   # Flyway 遷移腳本
│   │       └── templates/      # 郵件模板
│   └── test/                   # 測試代碼
├── scripts/                    # 部署腳本
├── docs/                       # 文件
├── docker-compose.yml          # 開發環境
├── docker-compose.prod.yml     # 生產環境
├── Dockerfile                  # Docker 映像
├── pom.xml                     # Maven 配置
└── README.md                   # 本文件
```

## 配置說明

### 郵件服務配置

#### Mailjet（推薦）

```bash
MAIL_PROVIDER=mailjet
MAILJET_API_KEY=your_api_key
MAILJET_SECRET_KEY=your_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com
MAILJET_FROM_NAME=Member Auth System
```

#### JavaMail (Gmail)

```bash
MAIL_PROVIDER=javamail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # 無空格
```

### 資料清理配置

```bash
# 啟用自動清理
CLEANUP_ENABLED=true

# 登入記錄保留天數
LOGIN_HISTORY_RETENTION_DAYS=90

# 清理任務執行時間（Cron 表達式）
LOGIN_HISTORY_CLEANUP_CRON=0 0 2 * * ?
```

##  故障排除

### 常見問題

**1. 郵件發送失敗**
- 檢查 `MAIL_PROVIDER` 設定
- 確認 API Key 或密碼正確
- Gmail 需要應用程式密碼，且密碼不能有空格

**2. 資料庫連線失敗**
- 確認 PostgreSQL 容器正在運行
- 檢查 `.env` 中的資料庫配置

**3. Redis 連線失敗**
- 確認 Redis 容器正在運行
- 檢查 Redis 密碼設定

**4. JWT Token 無效**
- 確認 `JWT_SECRET` 至少 256 位元
- 檢查 Token 是否過期

詳細故障排除請參考 [DEPLOYMENT.md](DEPLOYMENT.md#故障排除)

##  監控與日誌

### 健康檢查

```bash
curl http://localhost:8080/actuator/health
```

### 查看日誌

```bash
# Docker 日誌
docker-compose logs -f app

# 文件日誌
tail -f /var/lib/member-auth/logs/member-auth-system.log
```

### 監控指標

```bash
curl http://localhost:8080/actuator/metrics
```

##  作者

- **yaiiow159** - [GitHub](https://github.com/yaiiow159)




