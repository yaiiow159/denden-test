# Member Authentication System (會員認證系統)

安全的會員認證系統，提供註冊、雙因素認證登入與帳號查詢功能。

## 功能特性

- 會員註冊與 Email 驗證
- 雙因素認證登入 (密碼 + Email OTP)
- JWT Token 認證
- 帳號鎖定機制 (防暴力破解)
- Rate Limiting (API 限流)
- **動態郵件渠道切換** (Mailjet / JavaMail)
- Swagger API 文件

## 技術棧

- **Framework**: Spring Boot 3.2.0 (Java 17)
- **Security**: Spring Security 6.x + JWT
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Email**: Mailjet API / JavaMail (可切換)
- **Build**: Maven

### 前置需求

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Mailjet 帳號

### 本地開發

1. 複製環境變數範本：
```bash
cp .env.example .env
```

2. 編輯 `.env` 填入配置值

3. 建立資料庫：
```sql
CREATE DATABASE member_auth;
```

4. 啟動 Redis：
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

5. 執行應用程式：
```bash
mvn clean install
mvn spring-boot:run
```

應用程式啟動於 `http://localhost:8080`

### Docker Compose

```bash
cp .env.example .env
# 編輯 .env 填入 Mailjet 配置
docker-compose up -d
```


## 環境變數配置

### 必要配置

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=member_auth
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION_MS=86400000

# Mailjet
MAILJET_API_KEY=your_api_key
MAILJET_SECRET_KEY=your_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com
MAILJET_FROM_NAME=Member Auth System

# Application
APP_BASE_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

完整配置參考 `.env.example`

## API 文件

啟動後訪問 Swagger UI：
```
http://localhost:8080/swagger-ui.html
```

### 主要端點

**認證 API**
- `POST /api/v1/auth/register` - 註冊
- `GET /api/v1/auth/verify-email` - 驗證 Email
- `POST /api/v1/auth/login` - 登入（第一階段）
- `POST /api/v1/auth/verify-otp` - 驗證 OTP（第二階段）

**使用者 API** (需要 JWT Token)
- `GET /api/v1/users/me` - 取得使用者資訊
- `GET /api/v1/users/me/last-login` - 取得最後登入時間

詳細範例請參考 [docs/API_EXAMPLES.md](docs/API_EXAMPLES.md)

## EC2 部署

### 1. 準備 EC2 實例

```bash
# 安裝 Docker
sudo yum update -y
sudo yum install docker -y
sudo service docker start
sudo usermod -a -G docker ec2-user

# 安裝 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 部署應用

```bash
# 上傳專案到 EC2
scp -r . ec2-user@your-ec2-ip:/home/ec2-user/member-auth-system

# SSH 到 EC2
ssh ec2-user@your-ec2-ip

# 進入專案目錄
cd member-auth-system

# 配置環境變數
cp .env.example .env
nano .env  # 編輯配置

# 啟動服務
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 查看日誌
docker-compose logs -f app
```

### 3. 安全組設定

在 AWS Console 設定 EC2 安全組：
- 允許 TCP 8080 (應用程式)
- 允許 TCP 22 (SSH)
- 建議使用 ALB 並配置 HTTPS

### 4. 健康檢查

```bash
curl http://your-ec2-ip:8080/actuator/health
```


## 安全性

### 密碼要求
- 最少 8 字元
- 包含大寫、小寫、數字、特殊字元

### JWT Token
- 有效期：24 小時
- 使用方式：`Authorization: Bearer <token>`

### Rate Limiting
- 每 IP 每分鐘 10 次請求
- 超過限制回傳 HTTP 429

### 帳號鎖定
- 連續失敗 5 次鎖定 30 分鐘

詳細安全說明請參考 [docs/SECURITY_BEST_PRACTICES.md](docs/SECURITY_BEST_PRACTICES.md)

## 開發

### 執行測試
```bash
mvn test
```

### 建構 Docker 映像
```bash
docker build -t member-auth-system:1.0.0 .
```

### 查看日誌
```bash
# Docker
docker-compose logs -f app

# 本地
tail -f logs/application.log
```

## 故障排除

### 應用程式無法啟動
```bash
# 檢查資料庫
psql -h localhost -U postgres -d member_auth

# 檢查 Redis
redis-cli ping

# 檢查環境變數
docker-compose config
```

### 郵件發送失敗
```bash
# 查看日誌
docker-compose logs app | grep -i "email"

# 驗證 Mailjet 配置
curl -X GET https://api.mailjet.com/v3/REST/contact \
  -u "$MAILJET_API_KEY:$MAILJET_SECRET_KEY"
```

## 授權

Copyright © 2024 Denden Company. All rights reserved.

