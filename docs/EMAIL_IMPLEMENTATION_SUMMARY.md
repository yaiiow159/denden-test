# 郵件渠道動態切換實現總結

## 實現概述

基於**策略模式 (Strategy Pattern)** 設計，實現了可動態切換的郵件發送架構，支援 Mailjet 和 JavaMail（SMTP）兩種渠道，並可輕鬆擴展其他郵件服務。

## 核心組件

### 1. 策略介面層
- `EmailSender` - 郵件發送策略介面
- `EmailSendException` - 統一異常類

### 2. 策略實現層
- `MailjetEmailSender` - Mailjet API 實現
- `JavaMailEmailSender` - JavaMail SMTP 實現

### 3. 工廠管理層
- `EmailSenderFactory` - 策略工廠，管理和提供發送器

### 4. 配置層
- `MailjetConfig` - Mailjet 配置（條件化）
- `JavaMailConfig` - JavaMail 配置（條件化）

### 5. 業務層
- `EmailServiceImpl` - 重構為使用策略模式

## 文件結構

```
src/main/java/com/denden/auth/
├── service/
│   ├── email/
│   │   ├── EmailSender.java              # 策略介面
│   │   ├── EmailSendException.java       # 異常類
│   │   ├── MailjetEmailSender.java       # Mailjet 實現
│   │   ├── JavaMailEmailSender.java      # JavaMail 實現
│   │   └── EmailSenderFactory.java       # 工廠類
│   └── impl/
│       └── EmailServiceImpl.java         # 業務層（已重構）
└── config/
    ├── MailjetConfig.java                # Mailjet 配置
    └── JavaMailConfig.java               # JavaMail 配置

docs/
├── EMAIL_PROVIDER_GUIDE.md               # 完整使用指南
├── EMAIL_ARCHITECTURE.md                 # 架構設計文檔
├── EMAIL_PROVIDER_TESTING.md             # 測試指南
├── EMAIL_QUICK_REFERENCE.md              # 快速參考
└── EMAIL_IMPLEMENTATION_SUMMARY.md       # 本文檔
```

## 配置說明

### application.yml

```yaml
app:
  mail:
    provider: ${MAIL_PROVIDER:mailjet}  # 新增：渠道選擇
    from-name: ${MAIL_FROM_NAME:Member Auth System}
    mailjet:
      api-key: ${MAILJET_API_KEY:}
      secret-key: ${MAILJET_SECRET_KEY:}
      from-email: ${MAILJET_FROM_EMAIL:noreply@example.com}
      from-name: ${MAILJET_FROM_NAME:Member Auth System}

spring:
  mail:  # 新增：JavaMail 配置
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
```

### .env

```bash
# 新增：渠道選擇
MAIL_PROVIDER=mailjet  # 或 javamail

# Mailjet 配置
MAILJET_API_KEY=your_api_key
MAILJET_SECRET_KEY=your_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com

# JavaMail 配置（可選）
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### pom.xml

```xml
<!-- 新增依賴 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## 使用方式

### 切換到 Mailjet

```bash
export MAIL_PROVIDER=mailjet
# 或修改 .env
MAIL_PROVIDER=mailjet
```

### 切換到 JavaMail

```bash
export MAIL_PROVIDER=javamail
# 或修改 .env
MAIL_PROVIDER=javamail
```

### 運行時驗證

啟動日誌：
```
INFO  EmailSenderFactory - 郵件發送器初始化完成，當前使用: JAVAMAIL
INFO  EmailSenderFactory - 可用的發送器: [JAVAMAIL]
```

發送郵件：
```
INFO  EmailServiceImpl - 準備發送驗證郵件至: u***@example.com (使用: JAVAMAIL)
DEBUG JavaMailEmailSender - 使用 JavaMail 發送郵件至: u***@example.com
INFO  EmailServiceImpl - 驗證郵件發送成功至: u***@example.com
```

## 設計優勢

### 1. 符合 SOLID 原則

- **單一職責**：每個類別職責明確
- **開放封閉**：對擴展開放，對修改封閉
- **里氏替換**：所有實現可互相替換
- **介面隔離**：介面精簡，不強迫實現不需要的方法
- **依賴倒置**：依賴抽象而非具體實現

### 2. 配置驅動

- 使用 `@ConditionalOnProperty` 實現條件化 Bean
- 運行時根據配置自動選擇實現
- 無需修改代碼即可切換渠道

### 3. 易於擴展

新增郵件渠道只需：
1. 實現 `EmailSender` 介面
2. 創建對應的配置類
3. 添加配置項

無需修改現有代碼。

### 4. 生產就緒

- 非同步發送（`@Async`）
- 自動重試（`@Retryable`）
- 完整日誌記錄
- 敏感資訊遮罩

## 測試驗證

### 編譯測試

```bash
mvn clean compile -DskipTests
# [INFO] BUILD SUCCESS
```

### 單元測試（建議添加）

```java
@SpringBootTest
@TestPropertySource(properties = "app.mail.provider=javamail")
class EmailSenderFactoryTest {
    
    @Autowired
    private EmailSenderFactory factory;
    
    @Test
    void shouldUseJavaMailSender() {
        assertThat(factory.getActiveSender().getSenderType())
            .isEqualTo("JAVAMAIL");
    }
}
```

## 未來擴展方向

### 1. 新增郵件服務

- SendGrid
- AWS SES
- Azure Communication Services
- 阿里雲郵件推送

### 2. 高級功能

- 多渠道負載均衡
- 自動容錯切換
- 智能路由（根據收件者選擇渠道）
- 郵件佇列（RabbitMQ/Kafka）
- 發送統計和監控

### 3. 效能優化

- 批次發送
- 連接池優化
- 非同步佇列
- 限流保護

## 相關文檔

| 文檔 | 說明 |
|------|------|
| [EMAIL_PROVIDER_GUIDE.md](./EMAIL_PROVIDER_GUIDE.md) | 完整使用指南，包含配置、切換、故障排查 |
| [EMAIL_ARCHITECTURE.md](./EMAIL_ARCHITECTURE.md) | 架構設計詳解，包含類別圖、時序圖 |
| [EMAIL_PROVIDER_TESTING.md](./EMAIL_PROVIDER_TESTING.md) | 測試指南，包含單元測試、整合測試 |
| [EMAIL_QUICK_REFERENCE.md](./EMAIL_QUICK_REFERENCE.md) | 快速參考，常用配置和命令 |

## 變更記錄

### 新增文件
- `EmailSender.java` - 策略介面
- `EmailSendException.java` - 異常類
- `MailjetEmailSender.java` - Mailjet 實現
- `JavaMailEmailSender.java` - JavaMail 實現
- `EmailSenderFactory.java` - 工廠類
- `JavaMailConfig.java` - JavaMail 配置

### 修改文件
- `EmailServiceImpl.java` - 重構為使用策略模式
- `MailjetConfig.java` - 添加條件化配置
- `application.yml` - 添加郵件渠道配置
- `.env` - 添加渠道選擇和 JavaMail 配置
- `.env.example` - 更新配置說明
- `pom.xml` - 添加 spring-boot-starter-mail 依賴
- `README.md` - 更新功能說明

### 新增文檔
- `docs/EMAIL_PROVIDER_GUIDE.md`
- `docs/EMAIL_ARCHITECTURE.md`
- `docs/EMAIL_PROVIDER_TESTING.md`
- `docs/EMAIL_QUICK_REFERENCE.md`
- `docs/EMAIL_IMPLEMENTATION_SUMMARY.md`

## 總結

成功實現了基於策略模式的郵件渠道動態切換功能，具備以下特點：

✅ **靈活切換**：通過配置即可切換 Mailjet 和 JavaMail  
✅ **易於擴展**：新增渠道無需修改現有代碼  
✅ **生產就緒**：包含重試、非同步、日誌等機制  
✅ **架構優雅**：符合 SOLID 原則和設計模式最佳實踐  
✅ **文檔完善**：提供完整的使用和架構文檔  

當 Mailjet 臨時不可用時，可以快速切換到 JavaMail（Gmail、Outlook 等），保證郵件發送功能的高可用性。
