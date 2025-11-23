# GHCR 快速參考

## 一分鐘設定

### 1. 創建 Personal Access Token

```
GitHub → Settings → Developer settings → Personal access tokens
→ Generate new token (classic)
```

**權限**：
- ✅ `read:packages`
- ✅ `write:packages`

**複製 Token**：`ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 2. 添加到 GitHub Secrets

```
Repository → Settings → Secrets and variables → Actions
→ New repository secret
```

```
Name: GHCR_TOKEN
Value: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. 啟用 Workflow 權限

```
Repository → Settings → Actions → General
→ Workflow permissions
→ 選擇 "Read and write permissions"
```

## 驗證設定

### 推送後檢查

```
GitHub → Actions → 查看最新 workflow
→ "Build & Push" job 應該成功
```

### 查看 Package

```
GitHub Profile → Packages
→ 應該看到 member-auth-system
```

## Server 端使用

### 登入 GHCR

```bash
echo $GHCR_TOKEN | docker login ghcr.io -u <your-username> --password-stdin
```

### 拉取映像

```bash
docker pull ghcr.io/<your-username>/member-auth-system:latest
```

## 映像位置

```
ghcr.io/<your-username>/member-auth-system:latest
ghcr.io/<your-username>/member-auth-system:<commit-sha>
```

## 常見問題

### 推送失敗

檢查 Workflow permissions 是否設為 "Read and write"

### 拉取失敗

重新登入 GHCR：
```bash
echo $GHCR_TOKEN | docker login ghcr.io -u <username> --password-stdin
```

## 完整文檔

📖 [GHCR 設定指南](./GHCR_SETUP_GUIDE.md)
