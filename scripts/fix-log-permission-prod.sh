#!/bin/bash
# 生產環境日誌權限問題修復腳本

set -e

echo "=========================================="
echo "生產環境日誌權限問題修復腳本"
echo "=========================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 檢查是否在正確的目錄
if [ ! -f "docker-compose.yml" ] || [ ! -f "docker-compose.prod.yml" ]; then
    echo -e "${RED}錯誤: 找不到 docker-compose 配置文件${NC}"
    echo "請在專案根目錄執行此腳本"
    exit 1
fi

# 確認操作
echo -e "${YELLOW}警告: 此操作會重啟生產環境服務${NC}"
read -p "確定要繼續嗎？(yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "操作已取消"
    exit 0
fi

echo -e "${BLUE}[1/7] 停止容器...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

echo -e "${BLUE}[2/7] 創建並設置日誌目錄權限...${NC}"
sudo mkdir -p /var/lib/member-auth/logs
sudo chown -R $(id -u):$(id -g) /var/lib/member-auth/logs
sudo chmod 755 /var/lib/member-auth/logs
echo -e "${GREEN}日誌目錄已準備: /var/lib/member-auth/logs${NC}"

echo -e "${BLUE}[3/7] 清理舊映像...${NC}"
docker rmi ghcr.io/yaiiow159/member-auth-system:latest 2>/dev/null || echo "映像不存在，跳過"

echo -e "${BLUE}[4/7] 拉取最新映像...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull

echo -e "${BLUE}[5/7] 啟動容器...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

echo -e "${BLUE}[6/7] 等待服務啟動...${NC}"
sleep 15

echo -e "${BLUE}[7/7] 健康檢查...${NC}"
for i in {1..10}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 服務健康檢查通過${NC}"
        break
    else
        echo "等待服務啟動... ($i/10)"
        sleep 3
    fi
done

echo ""
echo -e "${GREEN}=========================================="
echo "修復完成！"
echo "==========================================${NC}"
echo ""

# 檢查容器狀態
echo -e "${BLUE}容器狀態：${NC}"
docker-compose ps

echo ""
echo -e "${BLUE}檢查日誌（最後 30 行）：${NC}"
docker-compose logs --tail=30 app

echo ""
echo -e "${BLUE}檢查主機日誌目錄：${NC}"
ls -lah /var/lib/member-auth/logs/

echo ""
if docker-compose logs app | grep -q "Permission denied"; then
    echo -e "${RED}⚠ 仍然存在權限錯誤，請檢查日誌${NC}"
    echo "執行以下命令查看詳細日誌："
    echo "  docker-compose logs app"
else
    echo -e "${GREEN}✓ 沒有發現權限錯誤${NC}"
fi

echo ""
echo "監控命令："
echo "  查看實時日誌: docker-compose logs -f app"
echo "  查看容器狀態: docker-compose ps"
echo "  查看日誌文件: ls -lah /var/lib/member-auth/logs/"
echo "  健康檢查: curl http://localhost:8080/actuator/health"
