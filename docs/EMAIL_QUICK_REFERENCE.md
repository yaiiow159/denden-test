# 郵件渠道切換快速參考

## 一分鐘快速切換

### 使用 Mailjet（預設）

```bash
# .env
MAIL_PROVIDER=mailjet
MAILJET_API_KEY=your_api_key
MAILJET_SECRET_KEY=your_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com
```

### 切換到 Gmail

```bash
# .env
MAIL_PROVIDER=javamail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

**Gmail 應用程式密碼設定**：https://myaccount.google.com/apppasswords

### 切換到 Outlook

```bash
# .env
MAIL_PROVIDER=javamail
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
MAIL_USERNAME=your-email@outlook.com
MAIL_PASSWORD=your-password
```

## 常用 SMTP 配置

| 服務商 | Host | Port | 說明 |
|--------|------|------|------|
| Gmail | smtp.gmail.com | 587 | 需要應用程式密碼 |
| Outlook | smtp-mail.outlook.com | 587 | 使用帳號密碼 |
| Yahoo | smtp.mail.yahoo.com | 587 | 需要應用程式密碼 |
| QQ Mail | smtp.qq.com | 587 | 需要授權碼 |
| 163 Mail | smtp.163.com | 465 | 需要授權碼 |
| AWS SES | email-smtp.us-east-1.amazonaws.com | 587 | 需要 SMTP 憑證 |

## Docker 環境切換

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - MAIL_PROVIDER=javamail
      - MAIL_HOST=smtp.gmail.com
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
```

## Kubernetes 切換

```bash
# 快速切換
kubectl set env deployment/member-auth-system \
  MAIL_PROVIDER=javamail \
  MAIL_HOST=smtp.gmail.com \
  MAIL_USERNAME=your-email@gmail.com \
  MAIL_PASSWORD=your-app-password

# 驗證
kubectl logs -f deployment/member-auth-system | grep "郵件發送器初始化"
```

## 驗證切換成功

啟動日誌應顯示：

```
INFO  EmailSenderFactory - 郵件發送器初始化完成，當前使用: JAVAMAIL
INFO  EmailSenderFactory - 可用的發送器: [JAVAMAIL]
```

發送郵件時：

```
INFO  EmailServiceImpl - 準備發送驗證郵件至: u***@example.com (使用: JAVAMAIL)
```

## 故障排查

### Mailjet 401 錯誤
```bash
# 檢查 API Key
echo $MAILJET_API_KEY
echo $MAILJET_SECRET_KEY
```

### JavaMail 連接超時
```bash
# 測試 SMTP 連接
telnet smtp.gmail.com 587

# 檢查防火牆
# Windows: netsh advfirewall firewall show rule name=all
# Linux: sudo iptables -L
```

### Gmail 認證失敗
1. 確認已啟用兩步驟驗證
2. 使用應用程式密碼，不是帳號密碼
3. 檢查「低安全性應用程式存取」設定

## 完整文檔

- 📧 [郵件發送渠道切換指南](./EMAIL_PROVIDER_GUIDE.md)
- 🏗️ [架構設計文檔](./EMAIL_ARCHITECTURE.md)
- 🧪 [測試指南](./EMAIL_PROVIDER_TESTING.md)
