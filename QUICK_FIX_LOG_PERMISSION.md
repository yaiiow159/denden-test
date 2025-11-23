# 日誌權限問題 - 立即修復

## 問題
容器啟動時出現：
```
java.io.FileNotFoundException: logs/member-auth-system-error.log (Permission denied)
```

## 原因
Docker 映像是舊版本，沒有包含日誌目錄的創建。

## 立即修復（3 個步驟）

### 步驟 1：停止並清理舊容器

```bash
# 停止所有容器
docker-compose down

# 清理舊映像（重要！）
docker rmi member-auth-app:latest
docker rmi ghcr.io/yaiiow159/member-auth-system:latest
```

### 步驟 2：重新構建映像

```bash
# 重新構建（不使用快取）
docker-compose build --no-cache app

# 或者如果使用 production 配置
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build --no-cache app
```

### 步驟 3：啟動容器

```bash
# 啟動
docker-compose up -d

# 或者使用 production 配置
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 驗證修復

```bash
# 檢查容器狀態
docker-compose ps

# 查看日誌（應該沒有權限錯誤）
docker-compose logs app

# 檢查日誌目錄
docker-compose exec app ls -la /app/logs/
```

## 如果還有問題

### 方案 A：手動創建日誌目錄

```bash
# 進入容器
docker-compose exec app sh

# 檢查目錄
ls -la /app/

# 如果 logs 目錄不存在或權限不對
mkdir -p /app/logs
chown spring:spring /app/logs
chmod 755 /app/logs

# 退出容器
exit

# 重啟容器
docker-compose restart app
```

### 方案 B：使用臨時解決方案（僅輸出到 console）

修改 `docker-compose.yml`，臨時使用 dev profile：

```yaml
environment:
  SPRING_PROFILES_ACTIVE: dev  # 改為 dev，只輸出到 console
```

然後重啟：
```bash
docker-compose restart app
```

## 生產環境修復

如果在生產環境遇到此問題：

```bash
# 1. 確保主機目錄存在且權限正確
sudo mkdir -p /var/lib/member-auth/logs
sudo chown -R $(id -u):$(id -g) /var/lib/member-auth/logs
sudo chmod 755 /var/lib/member-auth/logs

# 2. 停止容器
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

# 3. 拉取最新映像
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull

# 4. 重新啟動
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 5. 驗證
docker-compose logs app
ls -la /var/lib/member-auth/logs/
```

## CI/CD 自動部署修復

如果使用 GitHub Actions 部署，需要：

1. **推送代碼觸發新構建**：
```bash
git add .
git commit -m "fix: rebuild docker image with log directory"
git push origin main
```

2. **等待 CI/CD 完成**（會自動構建新映像）

3. **在伺服器上清理舊映像**：
```bash
ssh user@server
cd /opt/member-auth
docker-compose down
docker rmi ghcr.io/yaiiow159/member-auth-system:latest
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 預防措施

為避免將來出現類似問題：

1. **每次修改 Dockerfile 後都要重新構建**：
```bash
docker-compose build --no-cache
```

2. **使用版本標籤而不是 latest**：
```yaml
image: ghcr.io/yaiiow159/member-auth-system:v1.0.0
```

3. **在本地測試後再部署**：
```bash
# 本地測試
docker-compose up
# 確認沒問題後再推送
git push
```

## 檢查清單

- [ ] 停止舊容器
- [ ] 刪除舊映像
- [ ] 重新構建映像（使用 --no-cache）
- [ ] 啟動新容器
- [ ] 檢查日誌無錯誤
- [ ] 驗證日誌文件已創建

## 相關文檔

- [日誌權限問題詳細說明](docs/LOG_PERMISSION_FIX.md)
- [故障排除指南](docs/TROUBLESHOOTING.md)
