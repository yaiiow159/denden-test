# Requirements Document

## Introduction

本系統為一個安全的會員認證系統，提供會員註冊、雙因素登入與帳號查詢功能。系統採用 Email 作為唯一識別帳號，並實施 Email 驗證與雙階段認證機制以確保帳號安全性。系統整合 Mailjet API 進行 Email 寄送服務。

## Glossary

- **Member Auth System**: 會員認證系統，負責處理會員註冊、登入與帳號管理的後端服務
- **Email Verification**: Email 驗證機制，透過發送驗證連結確認使用者擁有該 Email 地址
- **Two-Factor Authentication (2FA)**: 雙因素認證，在密碼驗證後額外要求 Email OTP 驗證
- **OTP (One-Time Password)**: 一次性密碼，有時效性的驗證碼
- **Mailjet Service**: 第三方 Email 寄送服務提供商
- **JWT Token**: JSON Web Token，用於維持使用者登入狀態的加密令牌
- **Authenticated User**: 已通過完整認證流程的使用者
- **Verification Token**: 用於 Email 驗證的唯一識別碼
- **Login Session**: 使用者登入後的會話狀態

## Requirements

### Requirement 1: 會員註冊功能

**User Story:** 作為一個新使用者，我想要使用 Email 註冊帳號，以便我可以使用系統服務

#### Acceptance Criteria

1. WHEN 使用者提交註冊請求包含 Email 與密碼，THE Member Auth System SHALL 驗證 Email 格式的有效性
2. WHEN 使用者提交的 Email 已存在於系統中，THE Member Auth System SHALL 回傳錯誤訊息並拒絕註冊
3. WHEN 註冊資料驗證通過，THE Member Auth System SHALL 將密碼使用 BCrypt 演算法進行雜湊處理後儲存
4. WHEN 會員資料成功建立，THE Member Auth System SHALL 產生唯一的 Verification Token 並設定 24 小時有效期限
5. WHEN Verification Token 產生完成，THE Member Auth System SHALL 透過 Mailjet Service 發送包含驗證連結的 Email 至使用者信箱

### Requirement 2: Email 帳號開通功能

**User Story:** 作為一個已註冊的使用者，我想要透過 Email 驗證連結開通帳號，以便我可以登入系統

#### Acceptance Criteria

1. WHEN 使用者點擊 Email 中的驗證連結，THE Member Auth System SHALL 驗證 Verification Token 的有效性與時效性
2. IF Verification Token 已過期或無效，THEN THE Member Auth System SHALL 回傳錯誤訊息並拒絕開通
3. WHEN Verification Token 驗證通過，THE Member Auth System SHALL 將該會員帳號狀態更新為已啟用
4. WHEN 帳號啟用成功，THE Member Auth System SHALL 回傳成功訊息並引導使用者進行登入

### Requirement 3: 會員登入 - 第一階段密碼驗證

**User Story:** 作為一個已開通帳號的使用者，我想要使用 Email 與密碼登入，以便進入第二階段驗證

#### Acceptance Criteria

1. WHEN 使用者提交登入請求包含 Email 與密碼，THE Member Auth System SHALL 驗證該 Email 是否存在於系統中
2. IF 該 Email 不存在或帳號未啟用，THEN THE Member Auth System SHALL 回傳通用錯誤訊息避免帳號列舉攻擊
3. WHEN Email 存在且帳號已啟用，THE Member Auth System SHALL 使用 BCrypt 驗證密碼的正確性
4. IF 密碼驗證失敗，THEN THE Member Auth System SHALL 記錄失敗次數並在連續失敗 5 次後鎖定帳號 30 分鐘
5. WHEN 密碼驗證成功，THE Member Auth System SHALL 產生 6 位數字 OTP 並設定 5 分鐘有效期限
6. WHEN OTP 產生完成，THE Member Auth System SHALL 透過 Mailjet Service 發送 OTP 至使用者 Email

### Requirement 4: 會員登入 - 第二階段 Email OTP 驗證

**User Story:** 作為一個通過密碼驗證的使用者，我想要輸入 Email 收到的 OTP，以便完成登入流程

#### Acceptance Criteria

1. WHEN 使用者提交 OTP 驗證請求，THE Member Auth System SHALL 驗證 OTP 的正確性與時效性
2. IF OTP 已過期或錯誤，THEN THE Member Auth System SHALL 回傳錯誤訊息並允許使用者重新請求 OTP
3. IF OTP 連續錯誤 3 次，THEN THE Member Auth System SHALL 使該 OTP 失效並要求使用者重新進行第一階段驗證
4. WHEN OTP 驗證成功，THE Member Auth System SHALL 產生 JWT Token 包含使用者識別資訊並設定 24 小時有效期限
5. WHEN JWT Token 產生完成，THE Member Auth System SHALL 記錄當前登入時間至使用者資料中
6. WHEN 登入流程完成，THE Member Auth System SHALL 回傳 JWT Token 與使用者基本資訊

### Requirement 5: 查詢最後登入時間

**User Story:** 作為一個已登入的使用者，我想要查詢自己的最後登入時間，以便監控帳號使用狀況

#### Acceptance Criteria

1. WHEN 使用者發送查詢請求，THE Member Auth System SHALL 驗證請求中的 JWT Token 有效性
2. IF JWT Token 無效或已過期，THEN THE Member Auth System SHALL 回傳 401 未授權錯誤
3. WHEN JWT Token 驗證通過，THE Member Auth System SHALL 從 Token 中提取使用者識別資訊
4. WHEN 使用者識別資訊提取完成，THE Member Auth System SHALL 查詢該使用者的最後登入時間記錄
5. WHEN 查詢完成，THE Member Auth System SHALL 回傳最後登入時間資訊給請求者

### Requirement 6: 防止未授權查詢

**User Story:** 作為系統管理者，我想要確保使用者只能查詢自己的帳號資訊，以便保護使用者隱私

#### Acceptance Criteria

1. WHEN 使用者嘗試查詢其他使用者的登入時間，THE Member Auth System SHALL 驗證請求者身份與目標使用者身份是否一致
2. IF 請求者嘗試查詢非本人帳號資訊，THEN THE Member Auth System SHALL 回傳 403 禁止存取錯誤
3. WHEN 偵測到未授權查詢嘗試，THE Member Auth System SHALL 記錄該異常行為至安全日誌

### Requirement 7: Mailjet 整合

**User Story:** 作為系統開發者，我想要整合 Mailjet API 進行 Email 寄送，以便系統可以發送驗證與通知郵件

#### Acceptance Criteria

1. WHEN 系統需要發送 Email，THE Member Auth System SHALL 使用 Mailjet API 進行郵件發送
2. WHEN Mailjet API 呼叫失敗，THE Member Auth System SHALL 記錄錯誤並實施重試機制最多 3 次
3. IF 重試 3 次後仍失敗，THEN THE Member Auth System SHALL 記錄嚴重錯誤並通知系統管理者
4. WHEN Email 發送成功，THE Member Auth System SHALL 記錄發送日誌包含收件者與發送時間

### Requirement 8: API 文件提供

**User Story:** 作為 API 使用者，我想要有完整的 API 文件，以便我可以正確地呼叫系統 API

#### Acceptance Criteria

1. THE Member Auth System SHALL 提供 Swagger UI 介面展示所有 API 端點
2. THE Member Auth System SHALL 在 Swagger 文件中包含每個 API 的請求參數、回應格式與錯誤代碼說明
3. THE Member Auth System SHALL 提供可匯出的 OpenAPI 3.0 規格檔案

### Requirement 9: 安全性要求

**User Story:** 作為系統管理者，我想要系統具備基本的安全防護機制，以便保護使用者資料與系統安全

#### Acceptance Criteria

1. THE Member Auth System SHALL 對所有 API 端點實施 HTTPS 加密傳輸
2. THE Member Auth System SHALL 實施 Rate Limiting 限制每個 IP 每分鐘最多 10 次 API 請求
3. THE Member Auth System SHALL 在日誌中遮罩所有敏感資訊包含密碼、OTP 與 Token
4. THE Member Auth System SHALL 設定適當的 CORS 政策限制允許的來源網域
5. THE Member Auth System SHALL 實施 SQL Injection 防護使用參數化查詢
6. THE Member Auth System SHALL 在所有 API 回應中加入安全標頭包含 X-Content-Type-Options 與 X-Frame-Options
