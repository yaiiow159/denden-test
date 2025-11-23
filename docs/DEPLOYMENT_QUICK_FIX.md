# 部署問題快速修復指南

## 問題：找不到 docker-compose.yml

### 錯誤訊息
```
err: open /opt/member-auth/docker-compose.yml: no such file or directory
```

### 原因
CI/CD pipeline 只推送了 Docker 映像，但沒有將配置文件傳送到伺服器。

### 快速修復（3 步驟）

#### 步驟 1：手動上傳配置文件到伺服器

```bash
# 在本地執行
scp docker-compose.yml docker-compose.prod.yml .env.example user@server:/opt/member-auth/
```

#### 步驟 2：在伺服器上設置環境變數

```bash
# SSH 登入伺服器
ssh user@server

# 進入目錄
cd /opt/member-auth

# 創建 .env 文件（如果不存在）
cp .env.example .env

# 編輯 .env 填入實際值
nano .env
```

#### 步驟 3：手動部署一次

```bash
# 登入 GHCR
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin

# 拉取並啟動
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 檢查狀態
docker-compose ps
```

### 永久解決方案

已更新 `.github/workflows/deploy.yml`，現在會自動傳送配置文件。

**需要設置的 GitHub Secrets：**

1. 進入 GitHub 倉庫 → Settings → Secrets and variables → Actions
2. 添加以下 Secrets：

| Secret Name | 值 | 說明 |
|------------|-----|------|
| `SERVER_HOST` | `your-server-ip` | 伺服器 IP 或域名 |
| `SERVER_USER` | `ubuntu` | SSH 用戶名 |
| `SERVER_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----...` | SSH 私鑰完整內容 |
| `SERVER_PORT` | `22` | SSH 端口（可選，默認 22） |
| `GHCR_TOKEN` | `ghp_xxxxx` | GitHub Personal Access Token |

**生成 SSH 密鑰：**

```bash
# 生成密鑰對
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/deploy_key

# 將公鑰添加到伺服器
ssh-copy-id -i ~/.ssh/deploy_key.pub user@server

# 複製私鑰內容到 GitHub Secrets
cat ~/.ssh/deploy_key
```

**生成 GitHub Token：**

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token
3. 勾選權限：`read:packages`, `write:packages`
4. 複製 token 到 `GHCR_TOKEN` Secret

### 驗證修復

推送代碼到 main 分支，檢查 GitHub Actions：

```bash
git add .
git commit -m "fix: update deployment workflow"
git push origin main
```

在 GitHub 倉庫的 Actions 頁面查看部署進度。

## 其他常見問題

### 問題：權限被拒絕

```bash
# 確保目錄權限正確
sudo chown -R $USER:$USER /opt/member-auth
chmod 755 /opt/member-auth
```

### 問題：Docker 登入失敗

```bash
# 檢查 token 權限
# 確保 GHCR_TOKEN 有 read:packages 權限

# 手動測試登入
echo "YOUR_TOKEN" | docker login ghcr.io -u your-username --password-stdin
```

### 問題：容器無法啟動

```bash
# 查看日誌
docker-compose logs app

# 檢查環境變數
docker-compose config

# 重新構建
docker-compose down
docker-compose up -d --force-recreate
```

### 問題：資料庫連接失敗

```bash
# 檢查 .env 文件
cat .env | grep DB_

# 檢查 PostgreSQL 容器
docker-compose logs postgres

# 測試連接
docker-compose exec postgres psql -U admin -d member_auth -c "SELECT 1;"
```

## 緊急回滾

如果新版本有問題，快速回滾到上一個版本：

```bash
# 使用之前的映像 SHA
docker-compose down
docker pull ghcr.io/username/repo:previous-sha
docker-compose up -d
```

## 聯繫支援

如果問題仍未解決，請提供以下信息：

1. 錯誤訊息完整內容
2. `docker-compose ps` 輸出
3. `docker-compose logs app` 最後 50 行
4. `.env` 文件內容（隱藏敏感信息）
