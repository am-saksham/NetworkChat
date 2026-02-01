#!/bin/bash

# Configuration
SERVER_IP="52.66.246.194"      # e.g., 123.45.67.89
USER="ec2-user"                 # amazon linux default
REMOTE_DIR="~/chat-server"
PEM_KEY="/Volumes/SSD_MAC/chat-key.pem"        # Path to your .pem key

echo "--------------------------------------------------"
echo "🚀 Deploying NetworkChat to AWS ($SERVER_IP)..."
echo "--------------------------------------------------"

# 1. Upload Files (using rsync with PEM key)
rsync -avz -e "ssh -i $PEM_KEY" --exclude '.git' --exclude 'bin' --exclude '.settings' --exclude '.classpath' --exclude '.project' \
    ./ "$USER@$SERVER_IP:$REMOTE_DIR"

echo "--------------------------------------------------"
echo "✅ Upload Complete."
echo "--------------------------------------------------"
echo "To restart the server, run this command:"
echo "ssh -i $PEM_KEY $USER@$SERVER_IP 'sudo systemctl restart networkchat; sudo systemctl status networkchat --no-pager'"
echo "--------------------------------------------------"
