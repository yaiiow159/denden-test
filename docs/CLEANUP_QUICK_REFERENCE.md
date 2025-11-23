# 資料清理快速參考

## 一分鐘配置

### 預設配置（推薦）

```bash
# .env
CLEANUP_ENABLED=true
LOGIN_HISTORY_RETENTION_DAYS=90
```

系統會自動：
- 每天凌晨 2 點清理 90 天前的登入記錄
- 每天凌晨 3 點清理過期驗證 Token
- 每天凌晨 4 點清理舊的登入嘗試記錄

### 自定義配置

```bash
# 保留 30 天
LOGIN_HISTORY_RETENTION_DAYS=30

# 改為每天凌晨 1 點執行
LOGIN_HISTORY_CLEANUP_CRON=0 0 1 * * ?

# 停用清理功能
CLEANUP_ENABLED=false
```

## 常用 Cron 表達式

| 需求 | Cron 表達式 |
|------|-------------|
| 每天凌晨 2 點 | `0 0 2 * * ?` |
| 每 6 小時 | `0 0 */6 * * ?` |
| 每週日凌晨 3 點 | `0 0 3 * * SUN` |
| 每月 1 號午夜 | `0 0 0 1 * ?` |

## 驗證清理任務

### 查看日誌

```bash
# Docker
docker-compose logs app | grep "DataCleanupScheduler"

# 查看清理統計
docker-compose logs app | grep "清理完成"
```

### 預期日誌

```
INFO  DataCleanupScheduler - 開始執行登入記錄清理任務，保留天數: 90
INFO  LoginHistoryServiceImpl - 清理舊的登入記錄，刪除數量: 1523
INFO  DataCleanupScheduler - 登入記錄清理完成，刪除數量: 1523, 耗時: 45ms
```

## 手動觸發清理

### 通過 Service

```java
@Autowired
private LoginHistoryService loginHistoryService;

// 清理 30 天前的記錄
long removed = loginHistoryService.cleanOldLoginHistory(30);
```

### 檢查 Redis 資料

```bash
# 連接 Redis
redis-cli

# 查看登入記錄數量
> ZCARD login_history

# 查看最近 10 筆記錄
> ZRANGE login_history 0 10 WITHSCORES
```

## 容量規劃

| 使用者數 | 保留天數 | 預估記憶體 |
|---------|---------|-----------|
| 10 萬 | 90 天 | ~3 MB |
| 100 萬 | 90 天 | ~32 MB |
| 100 萬 | 30 天 | ~11 MB |
| 1000 萬 | 30 天 | ~107 MB |

## 故障排查

### 清理任務未執行

```bash
# 檢查配置
echo $CLEANUP_ENABLED

# 檢查時區
echo $TZ

# 查看定時任務狀態
curl http://localhost:8080/actuator/scheduledtasks
```

### 清理數量為 0

```bash
# 檢查 Redis 資料
redis-cli
> ZCARD login_history
> ZRANGE login_history 0 -1 WITHSCORES
```

## 完整文檔

📖 [資料清理定時任務指南](./DATA_CLEANUP_GUIDE.md)
