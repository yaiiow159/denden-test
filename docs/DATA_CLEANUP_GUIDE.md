# 資料清理定時任務指南

## 概述

為避免 Redis 和資料庫中的資料無限增長，系統實現了自動化的資料清理機制。

## 清理任務

### 1. 登入記錄清理（Redis ZSet）

**目的**：清理 Redis 中舊的登入記錄，避免記憶體占用過多

**執行時間**：每天凌晨 2:00（可配置）

**保留策略**：預設保留最近 90 天的記錄

**配置**：
```bash
# .env
CLEANUP_ENABLED=true
LOGIN_HISTORY_RETENTION_DAYS=90
LOGIN_HISTORY_CLEANUP_CRON=0 0 2 * * ?
```

**日誌範例**：
```
INFO  DataCleanupScheduler - 開始執行登入記錄清理任務，保留天數: 90
INFO  LoginHistoryServiceImpl - 清理舊的登入記錄，刪除數量: 1523, 截止時間: 2024-08-25T02:00:00
INFO  DataCleanupScheduler - 登入記錄清理完成，刪除數量: 1523, 耗時: 45ms
```

### 2. 過期驗證 Token 清理（資料庫）

**目的**：清理資料庫中已過期的 Email 驗證 Token

**執行時間**：每天凌晨 3:00（可配置）

**保留策略**：刪除已過期且超過 7 天的 Token

**配置**：
```bash
VERIFICATION_TOKEN_CLEANUP_CRON=0 0 3 * * ?
```

### 3. 登入嘗試記錄清理（資料庫）

**目的**：清理資料庫中舊的登入嘗試記錄

**執行時間**：每天凌晨 4:00（可配置）

**保留策略**：刪除超過 30 天的記錄

**配置**：
```bash
LOGIN_ATTEMPT_CLEANUP_CRON=0 0 4 * * ?
```

### 4. Redis 記憶體監控

**目的**：監控 Redis 記憶體使用情況，及時發現異常

**執行時間**：每小時（可配置）

**配置**：
```bash
MEMORY_MONITOR_CRON=0 0 * * * ?
```

## Cron 表達式說明

格式：`秒 分 時 日 月 星期`

### 常用範例

| 表達式 | 說明 |
|--------|------|
| `0 0 2 * * ?` | 每天凌晨 2:00 |
| `0 0 */6 * * ?` | 每 6 小時 |
| `0 0 0 * * ?` | 每天午夜 |
| `0 0 12 * * ?` | 每天中午 12:00 |
| `0 0 0 1 * ?` | 每月 1 號午夜 |
| `0 0 0 * * MON` | 每週一午夜 |

### 線上工具

- Cron 表達式生成器：https://crontab.guru/
- Spring Cron 測試：https://www.freeformatter.com/cron-expression-generator-quartz.html

## 配置調整

### 調整保留天數

```bash
# 保留 30 天
LOGIN_HISTORY_RETENTION_DAYS=30

# 保留 180 天
LOGIN_HISTORY_RETENTION_DAYS=180
```

### 調整執行時間

```bash
# 改為每天凌晨 1 點執行
LOGIN_HISTORY_CLEANUP_CRON=0 0 1 * * ?

# 改為每 12 小時執行一次
LOGIN_HISTORY_CLEANUP_CRON=0 0 */12 * * ?

# 改為每週日凌晨 3 點執行
LOGIN_HISTORY_CLEANUP_CRON=0 0 3 * * SUN
```

### 停用清理功能

```bash
# 停用所有清理任務
CLEANUP_ENABLED=false
```

## 手動觸發清理

### 方法 1：通過 API（建議添加）

```java
@RestController
@RequestMapping("/api/v1/admin/cleanup")
public class CleanupController {
    
    @PostMapping("/login-history")
    public ResponseEntity<?> cleanupLoginHistory(@RequestParam int daysAgo) {
        long removed = loginHistoryService.cleanOldLoginHistory(daysAgo);
        return ResponseEntity.ok(Map.of("removed", removed));
    }
}
```

### 方法 2：通過 Spring Boot Actuator

```bash
# 啟用 Actuator 端點
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,scheduledtasks

# 查看定時任務狀態
curl http://localhost:8080/actuator/scheduledtasks
```

### 方法 3：直接調用 Service

```java
@Autowired
private LoginHistoryService loginHistoryService;

// 清理 30 天前的記錄
long removed = loginHistoryService.cleanOldLoginHistory(30);
```

## 監控與告警

### 日誌監控

```bash
# 查看清理任務日誌
docker-compose logs app | grep "DataCleanupScheduler"

# 查看清理統計
docker-compose logs app | grep "清理完成"
```

### 建議監控指標

1. **清理數量異常**
   - 如果某次清理數量突然暴增，可能有異常登入行為
   
2. **清理失敗**
   - 監控清理任務失敗次數
   - 設置告警閾值

3. **Redis 記憶體使用**
   - 監控 Redis 記憶體使用率
   - 超過 80% 時告警

4. **清理耗時**
   - 監控清理任務執行時間
   - 耗時過長可能表示資料量過大

## 效能考量

### Redis ZSet 清理效能

```java
// 使用 removeRangeByScore 批次刪除，時間複雜度 O(log(N)+M)
// N = ZSet 總元素數，M = 刪除元素數
redisTemplate.opsForZSet().removeRangeByScore(key, 0, maxScore);
```

**效能測試**：
- 100 萬筆記錄，刪除 10 萬筆：約 50-100ms
- 1000 萬筆記錄，刪除 100 萬筆：約 500-1000ms

### 資料庫清理效能

```sql
-- 使用索引加速刪除
CREATE INDEX idx_created_at ON login_attempts(created_at);

-- 批次刪除避免長事務
DELETE FROM login_attempts 
WHERE created_at < NOW() - INTERVAL '30 days'
LIMIT 10000;
```

### 最佳實踐

1. **選擇低峰時段**
   - 預設凌晨 2-4 點執行
   - 避免影響業務高峰

2. **分批處理**
   - 大量資料分批刪除
   - 避免長時間鎖表

3. **監控執行時間**
   - 記錄每次清理耗時
   - 超過閾值時告警

4. **保留適當天數**
   - 根據業務需求調整
   - 平衡儲存成本和資料價值

## 故障排查

### 清理任務未執行

```bash
# 檢查配置
echo $CLEANUP_ENABLED

# 檢查 Cron 表達式
# 確認時區設置正確
echo $TZ

# 查看 Spring 定時任務狀態
curl http://localhost:8080/actuator/scheduledtasks
```

### 清理數量為 0

可能原因：
1. 資料都在保留期內
2. Redis 中沒有資料
3. 時間計算錯誤

```bash
# 檢查 Redis 中的資料
redis-cli
> ZCARD login_history
> ZRANGE login_history 0 10 WITHSCORES
```

### 清理失敗

```bash
# 查看錯誤日誌
docker-compose logs app | grep "清理任務執行失敗"

# 檢查 Redis 連接
redis-cli ping

# 檢查資料庫連接
psql -h localhost -U postgres -d member_auth
```

## 容量規劃

### Redis 記憶體估算

假設：
- 每個使用者 ID：8 bytes（Long）
- ZSet Score：8 bytes（Double）
- ZSet 開銷：約 16 bytes
- 總計：約 32 bytes/使用者

**容量計算**：
- 10 萬使用者：約 3.2 MB
- 100 萬使用者：約 32 MB
- 1000 萬使用者：約 320 MB

**建議**：
- 保留 90 天：適合中小型系統（< 100 萬使用者）
- 保留 30 天：適合大型系統（> 100 萬使用者）
- 保留 7 天：適合超大型系統（> 1000 萬使用者）

### 資料庫容量估算

**login_attempts 表**：
- 每筆記錄：約 200 bytes
- 每天 10 萬次登入：約 20 MB/天
- 保留 30 天：約 600 MB

**verification_tokens 表**：
- 每筆記錄：約 150 bytes
- 每天 1 萬次註冊：約 1.5 MB/天
- 保留 7 天：約 10.5 MB

## 進階配置

### 動態調整保留天數

```java
@RestController
@RequestMapping("/api/v1/admin/config")
public class ConfigController {
    
    @Value("${app.cleanup.login-history-retention-days}")
    private int retentionDays;
    
    @PostMapping("/retention-days")
    public ResponseEntity<?> updateRetentionDays(@RequestParam int days) {
        // 動態更新配置
        // 需要配合 Spring Cloud Config 或其他配置中心
        return ResponseEntity.ok(Map.of("retentionDays", days));
    }
}
```

### 條件化清理

```java
// 根據 Redis 記憶體使用率動態調整
if (redisMemoryUsage > 80%) {
    // 縮短保留期，加速清理
    loginHistoryService.cleanOldLoginHistory(30);
} else {
    // 正常保留期
    loginHistoryService.cleanOldLoginHistory(90);
}
```

## 總結

定時清理機制確保系統長期穩定運行：

✅ **自動化**：無需人工干預  
✅ **可配置**：靈活調整策略  
✅ **可監控**：完整日誌記錄  
✅ **高效能**：批次處理，低峰執行  
✅ **可擴展**：易於添加新的清理任務  

建議定期檢查清理日誌，根據實際情況調整保留策略。
