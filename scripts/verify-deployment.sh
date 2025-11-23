#!/bin/bash
# 部署驗證腳本 - 確保系統正常運行

set -e

echo "=========================================="
echo "Member Auth System - Deployment Verification"
echo "=========================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cd /opt/member-auth

# 1. 檢查容器狀態
echo -e "${GREEN}[1/5] 檢查容器狀態...${NC}"
if docker-compose ps | grep -q "Up"; then
    echo -e "${GREEN}✓ 容器正在運行${NC}"
    docker-compose ps
else
    echo -e "${RED}✗ 容器未運行${NC}"
    exit 1
fi

# 2. 檢查日誌目錄權限
echo -e "${GREEN}[2/5] 檢查日誌目錄權限...${NC}"
if [ -d "/var/lib/member-auth/logs" ]; then
    PERMS=$(stat -c "%a" /var/lib/member-auth/logs)
    echo "日誌目錄權限: $PERMS"
    if [ "$PERMS" = "777" ] || [ "$PERMS" = "755" ]; then
        echo -e "${GREEN}✓ 權限正確${NC}"
    else
        echo -e "${YELLOW}⚠ 權限可能不足，正在修復...${NC}"
        sudo chmod 777 /var/lib/member-auth/logs
        echo -e "${GREEN}✓ 權限已修復${NC}"
    fi
else
    echo -e "${RED}✗ 日誌目錄不存在${NC}"
    exit 1
fi

# 3. 檢查應用日誌是否有權限錯誤
echo -e "${GREEN}[3/5] 檢查應用日誌...${NC}"
if docker-compose logs app | grep -q "Permission denied"; then
    echo -e "${RED}✗ 發現權限錯誤${NC}"
    echo "錯誤日誌："
    docker-compose logs app | grep "Permission denied" | tail -5
    exit 1
else
    echo -e "${GREEN}✓ 無權限錯誤${NC}"
fi

# 4. 檢查健康狀態
echo -e "${GREEN}[4/5] 檢查應用健康狀態...${NC}"
MAX_RETRIES=5
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
        echo -e "${GREEN}✓ 健康檢查通過${NC}"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            echo -e "${YELLOW}健康檢查失敗，重試 $RETRY_COUNT/$MAX_RETRIES...${NC}"
            sleep 10
        else
            echo -e "${RED}✗ 健康檢查失敗${NC}"
            exit 1
        fi
    fi
done

# 5. 檢查日誌文件是否正常生成
echo -e "${GREEN}[5/5] 檢查日誌文件...${NC}"
if [ -f "/var/lib/member-auth/logs/member-auth-system.log" ]; then
    echo -e "${GREEN}✓ 日誌文件已生成${NC}"
    echo "最新日誌："
    tail -5 /var/lib/member-auth/logs/member-auth-system.log
else
    echo -e "${YELLOW}⚠ 日誌文件尚未生成（可能應用剛啟動）${NC}"
fi

echo ""
echo -e "${GREEN}=========================================="
echo "✓ 部署驗證完成！系統運行正常"
echo "==========================================${NC}"
echo ""
echo "查看即時日誌："
echo "  docker-compose logs -f app"
echo ""
echo "查看文件日誌："
echo "  tail -f /var/lib/member-auth/logs/member-auth-system.log"
