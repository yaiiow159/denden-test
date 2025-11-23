#!/bin/bash
# 伺服器初始化腳本
# 在 Vultr 虛擬機上執行此腳本來準備部署環境

set -e

echo "🚀 Setting up server for Member Auth System deployment..."

# 檢查是否為 root 用戶
if [ "$EUID" -ne 0 ]; then 
  echo "❌ Please run as root"
  exit 1
fi

# 更新系統
echo "📦 Updating system packages..."
apt-get update
apt-get upgrade -y

# 安裝必要工具
echo "🔧 Installing required tools..."
apt-get install -y \
  curl \
  wget \
  git \
  vim \
  htop \
  ufw \
  fail2ban

# 安裝 Docker
if ! command -v docker &> /dev/null; then
  echo "🐳 Installing Docker..."
  curl -fsSL https://get.docker.com -o get-docker.sh
  sh get-docker.sh
  rm get-docker.sh
  
  # 啟動 Docker 服務
  systemctl enable docker
  systemctl start docker
else
  echo "✅ Docker already installed"
fi

# 安裝 Docker Compose
if ! command -v docker-compose &> /dev/null; then
  echo "🐳 Installing Docker Compose..."
  DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d\" -f4)
  curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
else
  echo "✅ Docker Compose already installed"
fi

# 創建部署目錄
echo "📁 Creating deployment directories..."
mkdir -p /opt/member-auth
mkdir -p /var/lib/member-auth/{postgres,redis}
mkdir -p /var/log/member-auth
mkdir -p /opt/member-auth/backups/{postgres,redis}

# 設置目錄權限
chown -R 1000:1000 /var/lib/member-auth
chown -R 1000:1000 /var/log/member-auth
chown -R 1000:1000 /opt/member-auth/backups

# 配置防火牆
echo "🔥 Configuring firewall..."
ufw --force enable
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 8080/tcp  # 應用程式端口（可選，建議使用 Nginx 反向代理）

# 配置 fail2ban
echo "🛡️  Configuring fail2ban..."
systemctl enable fail2ban
systemctl start fail2ban

# 創建部署用戶（可選）
if ! id "deploy" &>/dev/null; then
  echo "👤 Creating deploy user..."
  useradd -m -s /bin/bash deploy
  usermod -aG docker deploy
  
  # 設置 SSH 密鑰目錄
  mkdir -p /home/deploy/.ssh
  chmod 700 /home/deploy/.ssh
  chown -R deploy:deploy /home/deploy/.ssh
  
  echo "⚠️  Please add your SSH public key to /home/deploy/.ssh/authorized_keys"
fi

# 配置 Docker 日誌輪轉
echo "📝 Configuring Docker log rotation..."
cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF

systemctl restart docker

# 安裝 Nginx（可選，用於反向代理）
read -p "Do you want to install Nginx as reverse proxy? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo "🌐 Installing Nginx..."
  apt-get install -y nginx
  
  # 創建基本的 Nginx 配置
  cat > /etc/nginx/sites-available/member-auth << 'EOF'
server {
    listen 80;
    server_name _;
    
    client_max_body_size 10M;
    
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        access_log off;
    }
}
EOF
  
  ln -sf /etc/nginx/sites-available/member-auth /etc/nginx/sites-enabled/
  rm -f /etc/nginx/sites-enabled/default
  
  nginx -t && systemctl restart nginx
  systemctl enable nginx
  
  echo "✅ Nginx configured successfully"
fi

# 顯示系統資訊
echo ""
echo "=========================================="
echo "✅ Server setup completed!"
echo "=========================================="
echo ""
echo "📊 System Information:"
echo "  - Docker version: $(docker --version)"
echo "  - Docker Compose version: $(docker-compose --version)"
echo "  - Deployment directory: /opt/member-auth"
echo "  - Data directory: /var/lib/member-auth"
echo "  - Log directory: /var/log/member-auth"
echo ""
echo "🔑 Next steps:"
echo "  1. Add your SSH public key to /home/deploy/.ssh/authorized_keys"
echo "  2. Configure GitHub Secrets in your repository:"
echo "     - SERVER_HOST: 139.180.195.36"
echo "     - SERVER_USER: deploy (or root)"
echo "     - SERVER_SSH_KEY: Your private SSH key"
echo "     - SERVER_PORT: 22 (default)"
echo "  3. Create .env file in /opt/member-auth with production values"
echo "  4. Push code to trigger deployment"
echo ""
echo "⚠️  Security recommendations:"
echo "  - Change SSH port from default 22"
echo "  - Disable root SSH login"
echo "  - Set up SSL/TLS with Let's Encrypt"
echo "  - Configure regular backups"
echo ""
