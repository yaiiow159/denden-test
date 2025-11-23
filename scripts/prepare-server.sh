#!/bin/bash
# 伺服器初始化腳本 - 準備部署環境

set -e

echo "=========================================="
echo "Member Auth System - Server Preparation"
echo "=========================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 檢查是否為 root 用戶
if [ "$EUID" -eq 0 ]; then 
    echo -e "${RED}請不要使用 root 用戶執行此腳本${NC}"
    echo "使用: sudo -u your-user ./prepare-server.sh"
    exit 1
fi

# 創建必要的目錄
echo -e "${GREEN}[1/5] 創建目錄結構...${NC}"
sudo mkdir -p /opt/member-auth
sudo mkdir -p /var/lib/member-auth/{postgres,redis,logs}

# 設置目錄所有權
echo -e "${GREEN}[2/5] 設置目錄權限...${NC}"
sudo chown -R $USER:$USER /opt/member-auth
sudo chown -R $USER:$USER /var/lib/member-auth

# 設置日誌目錄權限（允許 Docker 容器寫入）
sudo chmod 755 /var/lib/member-auth/logs

# 檢查 Docker 是否安裝
echo -e "${GREEN}[3/5] 檢查 Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Docker 未安裝，正在安裝...${NC}"
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo -e "${GREEN}Docker 安裝完成${NC}"
else
    echo -e "${GREEN}Docker 已安裝: $(docker --version)${NC}"
fi

# 檢查 Docker Compose 是否安裝
echo -e "${GREEN}[4/5] 檢查 Docker Compose...${NC}"
if ! command -v docker-compose &> /dev/null; then
    echo -e "${YELLOW}Docker Compose 未安裝，正在安裝...${NC}"
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo -e "${GREEN}Docker Compose 安裝完成${NC}"
else
    echo -e "${GREEN}Docker Compose 已安裝: $(docker-compose --version)${NC}"
fi

# 創建 .env 文件模板（如果不存在）
echo -e "${GREEN}[5/5] 檢查環境變數文件...${NC}"
if [ ! -f /opt/member-auth/.env ]; then
    echo -e "${YELLOW}創建 .env 文件模板...${NC}"
    cat > /opt/member-auth/.env << 'EOF'
# Database Configuration
POSTGRES_DB=member_auth
POSTGRES_USER=admin
POSTGRES_PASSWORD=CHANGE_ME_STRONG_PASSWORD

# Redis Configuration
REDIS_PASSWORD=CHANGE_ME_REDIS_PASSWORD

# JWT Configuration
JWT_SECRET=CHANGE_ME_JWT_SECRET_MIN_256_BITS
JWT_EXPIRATION_MS=86400000

# Email Configuration (Mailjet)
MAILJET_API_KEY=your_mailjet_api_key
MAILJET_SECRET_KEY=your_mailjet_secret_key
MAILJET_FROM_EMAIL=noreply@yourdomain.com
MAILJET_FROM_NAME=Member Auth System

# Application Configuration
APP_BASE_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# GitHub Repository (for GHCR)
GITHUB_REPOSITORY=yaiiow159/member-auth-system
EOF
    chmod 600 /opt/member-auth/.env
    echo -e "${YELLOW}⚠️  請編輯 /opt/member-auth/.env 並填入實際值！${NC}"
else
    echo -e "${GREEN}.env 文件已存在${NC}"
fi

# 顯示目錄結構
echo ""
echo -e "${GREEN}=========================================="
echo "目錄結構已創建："
echo "==========================================${NC}"
tree -L 2 /opt/member-auth /var/lib/member-auth 2>/dev/null || ls -la /opt/member-auth /var/lib/member-auth

echo ""
echo -e "${GREEN}=========================================="
echo "準備完成！"
echo "==========================================${NC}"
echo ""
echo -e "${YELLOW}注意: 如果剛安裝 Docker，請登出並重新登入以使用戶組生效${NC}"
