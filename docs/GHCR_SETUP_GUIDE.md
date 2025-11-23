# GitHub Container Registry (GHCR) 設定指南

## 概述

本專案使用 GitHub Container Registry (GHCR) 作為 Docker 映像倉庫。GHCR 是 GitHub 提供的免費容器鏡像服務。

## 映像位置

```
ghcr.io/<your-username>/<repository-name>:latest
ghcr.io/<your-username>/<repository-name>:<commit-sha>
```

範例：
```
ghcr.io/denden/member-auth-system:latest
ghcr.io/denden/member-auth-system:abc123
```

## 設定步驟

### 1. 啟用 GitHub Actions 寫入權限

**路徑**：`Repository → Settings → Actions → General`

**設定**：
```
Workflow permissions
→ 選擇 "Read and write permissions"
→ 勾選 "Allow GitHub Actions to create and approve pull requests"
```

**截圖位置**：
```
https://github.com/<username>/<repo>/settings/actions
```

### 2. 創建 Personal Access Token (PAT)

**用途**：讓 server 端可以拉取 GHCR 映像

**步驟**：

1. 前往 GitHub Settings
   ```
   https://github.com/settings/tokens
   ```

2. 點擊 "Generate new token" → "Generate new token (classic)"

3. 設定 Token：
   - **Note**: `GHCR Token for member-auth-system`
   - **Expiration**: 選擇適當的過期時間（建議 90 天或 1 年）
   - **Scopes**: 勾選以下權限
     - ✅ `read:packages` - 讀取 packages
     - ✅ `write:packages` - 寫入 packages
     - ✅ `delete:packages` - 刪除 packages（可選）

4. 點擊 "Generate token"

5. **重要**：複製 Token（只會顯示一次）
   ```
   ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```

### 3. 添加 GitHub Secret

**路徑**：`Repository → Settings → Secrets and variables → Actions`

**新增 Secret**：
```
Name: GHCR_TOKEN
Value: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**其他必要的 Secrets**：
```
SERVER_HOST=139.180.195.36
SERVER_USER=deploy
SERVER_SSH_KEY=<your-ssh-private-key>
SERVER_PORT=22
```

### 4. 設定 Package 可見性（可選）

首次推送後，package 預設是 private。

**設為 Public**：

1. 前往你的 GitHub Profile
   ```
   https://github.com/<username>?tab=packages
   ```

2. 選擇 `member-auth-system` package

3. 點擊 "Package settings"

4. 在 "Danger Zone" 中選擇 "Change visibility"

5. 選擇 "Public" 並確認

**注意**：Public package 任何人都可以拉取，但只有你可以推送。

## Workflow 配置說明

### 環境變數

```yaml
env:
  REGISTRY: ghcr.io  # GitHub Container Registry
  IMAGE_NAME: ${{ github.repository }}  # 自動使用 repo 名稱
```

### 登入 GHCR

```yaml
- name: Log in to GHCR
  uses: docker/login-action@v3
  with:
    registry: ${{ env.REGISTRY }}
    username: ${{ github.actor }}  # 你的 GitHub 使用者名稱
    password: ${{ secrets.GITHUB_TOKEN }}  # GitHub 自動提供
```

### 推送映像

```yaml
- name: Build and push
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: |
      ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
      ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

### Server 端拉取

```yaml
# 登入 GHCR（使用 PAT）
echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io -u "${{ github.actor }}" --password-stdin

# 拉取映像
docker-compose pull
```

## 本地開發使用 GHCR

### 1. 登入 GHCR

```bash
# 使用 PAT 登入
echo $GHCR_TOKEN | docker login ghcr.io -u <your-username> --password-stdin
```

### 2. 拉取映像

```bash
docker pull ghcr.io/<your-username>/member-auth-system:latest
```

### 3. 推送映像（手動）

```bash
# 構建映像
docker build -t ghcr.io/<your-username>/member-auth-system:latest .

# 推送映像
docker push ghcr.io/<your-username>/member-auth-system:latest
```

## Server 端設定

### 1. 在 Server 上登入 GHCR

```bash
# SSH 到 server
ssh deploy@139.180.195.36

# 登入 GHCR
echo $GHCR_TOKEN | docker login ghcr.io -u <your-username> --password-stdin

# 驗證登入
docker pull ghcr.io/<your-username>/member-auth-system:latest
```

### 2. 更新 docker-compose.yml

確保使用正確的映像名稱：

```yaml
services:
  app:
    image: ghcr.io/${GITHUB_REPOSITORY}:latest
    # 或直接指定
    # image: ghcr.io/<your-username>/member-auth-system:latest
```

### 3. 設定環境變數

```bash
# 在 server 上設定
export GITHUB_REPOSITORY=<your-username>/member-auth-system

# 或在 .env 中
echo "GITHUB_REPOSITORY=<your-username>/member-auth-system" >> .env
```

## 驗證設定

### 1. 檢查 Workflow 執行

```
GitHub Repository → Actions → 選擇最新的 workflow run
→ 查看 "Build & Push" job
→ 確認 "Build and push" step 成功
```

### 2. 檢查 Package

```
GitHub Profile → Packages
→ 應該看到 member-auth-system package
→ 點擊進入查看 tags
```

### 3. 測試拉取

```bash
# 本地測試
docker pull ghcr.io/<your-username>/member-auth-system:latest

# Server 測試
ssh deploy@139.180.195.36
docker pull ghcr.io/<your-username>/member-auth-system:latest
```

## 常見問題

### Q1: 推送失敗 "denied: permission_denied"

**原因**：沒有寫入權限

**解決**：
1. 檢查 Workflow permissions 是否設為 "Read and write"
2. 確認 `permissions: packages: write` 已設定

### Q2: Server 拉取失敗 "unauthorized"

**原因**：未登入或 Token 過期

**解決**：
```bash
# 重新登入
echo $GHCR_TOKEN | docker login ghcr.io -u <your-username> --password-stdin
```

### Q3: Package 不存在

**原因**：首次推送尚未完成

**解決**：
1. 等待 GitHub Actions workflow 完成
2. 檢查 Actions 日誌確認推送成功
3. 刷新 Packages 頁面

### Q4: GITHUB_TOKEN 在 server 端失效

**原因**：`GITHUB_TOKEN` 只在 workflow 執行期間有效

**解決**：使用 PAT（Personal Access Token）
```yaml
# 使用 GHCR_TOKEN 而非 GITHUB_TOKEN
echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io ...
```

### Q5: 映像名稱不正確

**檢查**：
```bash
# 查看實際的映像名稱
docker images | grep ghcr.io

# 應該是
ghcr.io/<username>/<repo>:latest
```

## 替代方案

如果不想使用 GHCR，可以改用：

### 1. Docker Hub

```yaml
env:
  REGISTRY: docker.io
  IMAGE_NAME: <your-dockerhub-username>/member-auth-system
```

需要設定 Secrets：
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

### 2. 私有 Registry

```yaml
env:
  REGISTRY: registry.yourdomain.com
  IMAGE_NAME: member-auth-system
```

需要設定認證資訊。

### 3. 不使用 Registry（直接構建）

在 server 上直接構建，不推送到 registry：

```yaml
script: |
  cd /opt/member-auth
  git pull
  docker-compose build
  docker-compose up -d
```

## 成本

**GHCR 免費額度**：
- Public repositories: 無限制
- Private repositories: 
  - 500 MB 儲存空間
  - 1 GB 流量/月

**超過額度**：
- 儲存：$0.25/GB/月
- 流量：$0.50/GB

對於小型專案，免費額度通常足夠。

## 安全建議

1. **使用 PAT 而非密碼**
   - PAT 可以設定過期時間
   - PAT 可以限制權限範圍

2. **定期輪換 Token**
   - 建議每 90 天更新一次
   - 在 GitHub 設定提醒

3. **最小權限原則**
   - 只給予必要的權限
   - 生產環境使用只讀 Token

4. **Private Package**
   - 敏感專案使用 private package
   - 控制存取權限

## 總結

使用 GHCR 的優勢：

✅ **免費** - GitHub 提供的免費服務  
✅ **整合** - 與 GitHub Actions 無縫整合  
✅ **簡單** - 無需額外註冊和配置  
✅ **安全** - 使用 GitHub 的認證機制  
✅ **快速** - GitHub 的全球 CDN  

只需要：
1. 啟用 Workflow 寫入權限
2. 創建 PAT 並添加到 Secrets
3. 確保 docker-compose.yml 使用正確的映像名稱

就可以開始使用了！
