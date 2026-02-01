#!/bin/bash

# Configuration
SERVER_IP="52.66.246.194"
USER="ec2-user"
PEM_KEY="/Volumes/SSD_MAC/chat-key.pem"

echo "--------------------------------------------------"
echo "🚀 Setting up Systemd on AWS ($SERVER_IP)..."
echo "--------------------------------------------------"

ssh -i "$PEM_KEY" "$USER@$SERVER_IP" 'bash -s' << 'EOF'
    # 1. Stop any running nohup server
    echo "Stopping existing server..."
    pkill -f ServerMain || echo "No server running (that is fine)."

    # 2. Create the Service File
    echo "Creating service file..."
    sudo bash -c 'cat > /etc/systemd/system/networkchat.service << EOS
[Unit]
Description=Network Chat Server
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/chat-server
ExecStart=/bin/bash /home/ec2-user/chat-server/run_server.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOS'

    # 3. Enable and Start
    echo "Enabling and Starting service..."
    sudo systemctl daemon-reload
    sudo systemctl enable networkchat
    sudo systemctl restart networkchat

    # 4. Check Status
    echo "Checking Status..."
    sudo systemctl status networkchat --no-pager
EOF

echo "--------------------------------------------------"
echo "✅ Systemd Setup Complete!"
echo "Server will now auto-restart on crashes and boot."
echo "--------------------------------------------------"
