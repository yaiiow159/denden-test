# 部署設置指南

## 伺服器初始設置

### 1. 安裝必要軟體

```bash
# 更新系統
sudo apt update && sudo apt upgrade -y

# 安裝 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安裝 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 驗證安裝
docker --version
docker-compose --version
```

### 2. 創建部署目錄

```bash
# 創建應用目錄
sudo mkdir -p /opt/member-auth
sudo chown $USER:$USER /opt/member-auth
cd /opt/member-auth
```

### 3. 設置環境變數

```bash
# 創建 .env 文件
cat > .env << 'EOF'
# Database Configuration
DB_HOST=postgres
DB_PORT=5432
DB_NAME=member_auth
DB_USERNAME=admin
DB_PASSWORD=your_secure_password_here

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password_here

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_min_256_bits_required_here
JWT_EXPIRATION_MS=86400000

# Email Configuration (Mailjet)
MAIL_PROVIDER=mailjet
MAILJET_API_KEY=your_mailjet_api_key
MAILJET_SECRET_KEY=your_mailjet_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com
MAILJET_FROM_NAME=Member Auth System

# Application Configuration
APP_BASE_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
EOF

# 設置權限
chmod 600 .env
```

## 部署方式選擇

### 方式 1：SCP 傳送配置文件（推薦）

**優點：**
- 簡單直接
- 不需要在伺服器上 clone 倉庫
- 配置文件自動更新

**設置步驟：**

1. 確保 GitHub Actions 有 SSH 訪問權限
2. 配置 GitHub Secrets：
   - `SERVER_HOST`: 伺服器 IP 或域名
   - `SERVER_USER`: SSH 用戶名
   - `SERVER_SSH_KEY`: SSH 私鑰
   - `SERVER_PORT`: SSH 端口（默認 22）
   - `GHCR_TOKEN`: GitHub Personal Access Token

3. 使用當前的 `.github/workflows/deploy.yml`

### 方式 2：Git Pull 方式

**優點：**
- 可以追蹤配置文件變更歷史
- 方便手動回滾

**設置步驟：**

1. 在伺服器上 clone 倉庫：

```bash
cd /opt/member-auth
git clone https://github.com/your-username/your-repo.git .
```

2. 設置 Git 憑證（如果是私有倉庫）：

```bash
# 使用 Personal Access Token
git config credential.helper store
echo "https://username:token@github.com" > ~/.git-credentials
```

3. 使用 `.github/workflows/deploy-git-pull.yml.example`

## GitHub Secrets 設置

在 GitHub 倉庫設置中添加以下 Secrets：

### 必需的 Secrets

| Secret Name | 說明 | 範例 |
|------------|------|------|
| `SERVER_HOST` | 伺服器 IP 或域名 | `123.456.789.0` 或 `server.example.com` |
| `SERVER_USER` | SSH 用戶名 | `ubuntu` 或 `deploy` |
| `SERVER_SSH_KEY` | SSH 私鑰 | 完整的私鑰內容 |
| `GHCR_TOKEN` | GitHub Personal Access Token | `ghp_xxxxx` |

### 可選的 Secrets

| Secret Name | 說明 | 默認值 |
|------------|------|--------|
| `SERVER_PORT` | SSH 端口 | `22` |

## SSH 密鑰生成

```bash
# 在本地生成 SSH 密鑰對
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key

# 將公鑰添加到伺服器
ssh-copy-id -i ~/.ssh/deploy_key.pub user@server

# 將私鑰內容複製到 GitHub Secrets
cat ~/.ssh/deploy_key
```

## 手動部署測試

在設置 CI/CD 之前，先手動測試部署流程：

```bash
# 登入伺服器
ssh user@server

# 進入部署目錄
cd /opt/member-auth

# 確保配置文件存在
ls -la docker-compose.yml docker-compose.prod.yml .env

# 登入 GHCR
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin

# 拉取映像
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull

# 啟動服務
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 檢查狀態
docker-compose ps

# 查看日誌
docker-compose logs -f app
```

## 健康檢查

部署完成後，驗證服務是否正常運行：

```bash
# 檢查容器狀態
docker-compose ps

# 檢查應用健康狀態
curl http://localhost:8080/actuator/health

# 檢查 API 文檔
curl http://localhost:8080/swagger-ui.html
```

## 故障排除

### 問題 1：找不到 docker-compose.yml

**錯誤訊息：**
```
open /opt/member-auth/docker-compose.yml: no such file or directory
```

**解決方案：**
- 確保使用方式 1（SCP）時，`appleboy/scp-action` 正確配置
- 確保使用方式 2（Git Pull）時，倉庫已正確 clone

### 問題 2：權限被拒絕

**錯誤訊息：**
```
Permission denied (publickey)
```

**解決方案：**
- 檢查 SSH 密鑰是否正確添加到 GitHub Secrets
- 確保公鑰已添加到伺服器的 `~/.ssh/authorized_keys`
- 檢查 SSH 端口是否正確

### 問題 3：Docker 登入失敗

**錯誤訊息：**
```
Error response from daemon: Get "https://ghcr.io/v2/": unauthorized
```

**解決方案：**
- 確保 `GHCR_TOKEN` 有正確的權限（`read:packages`）
- 檢查 token 是否過期
- 確保映像名稱正確（`ghcr.io/username/repo:tag`）

### 問題 4：容器無法啟動

**檢查步驟：**

```bash
# 查看容器日誌
docker-compose logs app

# 檢查環境變數
docker-compose config

# 檢查網絡連接
docker network ls
docker network inspect member-auth-network
```

## 回滾策略

如果部署出現問題，可以快速回滾：

```bash
# 使用特定版本的映像
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
docker pull ghcr.io/username/repo:previous-sha
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 或使用 Git 回滾（方式 2）
git checkout previous-commit
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 監控和日誌

### 查看實時日誌

```bash
# 所有服務
docker-compose logs -f

# 特定服務
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

### 日誌持久化

日誌已配置為持久化到 `./logs` 目錄：

```bash
# 查看應用日誌
tail -f logs/application.log

# 查看錯誤日誌
tail -f logs/error.log
```

## 安全建議

1. **定期更新密碼和密鑰**
2. **使用防火牆限制訪問**
3. **啟用 HTTPS（使用 Nginx + Let's Encrypt）**
4. **定期備份資料庫**
5. **監控資源使用情況**
6. **設置日誌輪轉**

## 自動化備份

創建備份腳本：

```bash
#!/bin/bash
# /opt/member-auth/backup.sh

BACKUP_DIR="/opt/backups/member-auth"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 備份資料庫
docker-compose exec -T postgres pg_dump -U admin member_auth | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# 保留最近 7 天的備份
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/db_$DATE.sql.gz"
```

設置 cron job：

```bash
# 每天凌晨 2 點執行備份
0 2 * * * /opt/member-auth/backup.sh >> /var/log/member-auth-backup.log 2>&1
```
