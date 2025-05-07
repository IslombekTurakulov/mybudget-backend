#!/bin/bash

# Exit on error
set -e

# Wait for cloud-init to finish
echo "Waiting for cloud-init to finish..."
while [ ! -f /var/lib/cloud/instance/boot-finished ]; do
    echo "Cloud-init is still running..."
    sleep 10
done

# Create application directory
sudo mkdir -p /opt/mybudget/backend
sudo chown -R $USER:$USER /opt/mybudget

# Install Docker if not installed
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    # Remove any existing Docker packages
    sudo apt-get remove -y docker docker-engine docker.io containerd runc || true
    
    # Update package index
    sudo apt-get update
    
    # Install prerequisites
    sudo apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release

    # Add Docker's official GPG key
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

    # Set up the stable repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker Engine
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io

    # Add user to docker group
    sudo usermod -aG docker $USER
fi

# Install Docker Compose if not installed
if ! command -v docker-compose &> /dev/null; then
    echo "Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# Create necessary directories
mkdir -p /opt/mybudget/backend/secrets

# Set proper permissions
chmod 700 /opt/mybudget/backend/secrets

echo "Server setup completed successfully!"
echo "Please make sure to:"
echo "1. Add your SSH public key to the server"
echo "2. Configure GitHub secrets with the server details"
echo "3. Copy docker-compose.yml to /opt/mybudget/backend/" 