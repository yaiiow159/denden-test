# Member Authentication System (會員認證系統)

安全的會員認證系統，提供註冊、雙因素認證登入與帳號查詢功能。

## 功能特性

- 會員註冊與 Email 驗證
- 雙因素認證登入 (密碼 + Email OTP)
- JWT Token 認證
- 帳號鎖定機制 (防暴力破解)
- Rate Limiting (API 限流)
- **動態郵件渠道切換** (Mailjet / JavaMail)
- **自動資料清理** (定時清理 Redis 和資料庫舊資料)
- Swagger API 文件

## 技術棧

- **Framework**: Spring Boot 3.2.0 (Java 17)
- **Security**: Spring Security 6.x + JWT
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Email**: JavaMail (預設) / Mailjet (可切換)
- **Build**: Maven

### 前置需求

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- SMTP 郵件服務（Gmail / Outlook 等）或 Mailjet 帳號

### 主要端點

**認證 API**
- `POST /api/v1/auth/register` - 註冊
- `GET /api/v1/auth/verify-email` - 驗證 Email
- `POST /api/v1/auth/login` - 登入（第一階段）
- `POST /api/v1/auth/verify-otp` - 驗證 OTP（第二階段）

**使用者 API** (需要 JWT Token)
- `GET /api/v1/users/me` - 取得使用者資訊
- `GET /api/v1/users/me/last-login` - 取得最後登入時間



