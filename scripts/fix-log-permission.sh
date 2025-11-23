#!/bin/bash
# 日誌權限問題一鍵修復腳本

set -e

echo "=========================================="
echo "日誌權限問題修復腳本"
echo "=========================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 檢查是否在正確的目錄
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}錯誤: 找不到 docker-compose.yml${NC}"
    echo "請在專案根目錄執行此腳本"
    exit 1
fi

echo -e "${BLUE}[1/6] 停止容器...${NC}"
docker-compose down || true

echo -e "${BLUE}[2/6] 清理舊映像...${NC}"
docker rmi member-auth-app:latest 2>/dev/null || echo "本地映像不存在，跳過"
docker rmi ghcr.io/yaiiow159/member-auth-system:latest 2>/dev/null || echo "GHCR 映像不存在，跳過"

echo -e "${BLUE}[3/6] 確保主機日誌目錄存在...${NC}"
if [ -d "/var/lib/member-auth" ]; then
    sudo mkdir -p /var/lib/member-auth/logs
    sudo chown -R $(id -u):$(id -g) /var/lib/member-auth/logs
    sudo chmod 755 /var/lib/member-auth/logs
    echo -e "${GREEN}生產環境日誌目錄已準備${NC}"
fi

echo -e "${BLUE}[4/6] 重新構建映像（不使用快取）...${NC}"
docker-compose build --no-cache app

echo -e "${BLUE}[5/6] 啟動容器...${NC}"
docker-compose up -d

echo -e "${BLUE}[6/6] 等待容器啟動...${NC}"
sleep 10

echo ""
echo -e "${GREEN}=========================================="
echo "修復完成！"
echo "==========================================${NC}"
echo ""

# 檢查容器狀態
echo -e "${BLUE}容器狀態：${NC}"
docker-compose ps

echo ""
echo -e "${BLUE}檢查日誌（最後 20 行）：${NC}"
docker-compose logs --tail=20 app

echo ""
echo -e "${BLUE}檢查日誌目錄：${NC}"
docker-compose exec app ls -la /app/logs/ || echo -e "${YELLOW}無法檢查日誌目錄，容器可能還在啟動${NC}"

echo ""
echo -e "${GREEN}如果沒有看到權限錯誤，修復成功！${NC}"
echo ""
echo "後續步驟："
echo "1. 查看完整日誌: docker-compose logs -f app"
echo "2. 測試 API: curl http://localhost:8080/actuator/health"
echo "3. 查看 Swagger: http://localhost:8080/swagger-ui.html"
