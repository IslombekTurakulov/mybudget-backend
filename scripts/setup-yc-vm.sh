#!/bin/bash

# Exit on error
set -e

# Check if yc is installed
if ! command -v yc &> /dev/null; then
    echo "Installing Yandex Cloud CLI..."
    curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash
    yc init
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Installing jq..."
    brew install jq
fi

# Variables
VM_NAME="mybudget-backend"
FOLDER_ID=$(yc config get folder-id)
ZONE="ru-central1-a"
SUBNET_NAME="default-ru-central1-a"
# Using Ubuntu 22.04 LTS image ID from Yandex Cloud
IMAGE_ID="fd80qm01ah03dkqb14lc"  # Ubuntu 22.04 LTS
CORES=2
MEMORY=2  # Reduced to 2GB as it's enough for our needs
DISK_SIZE=30  # Minimum required size for Ubuntu
VM_USER="ubuntu"  # Default user for Ubuntu image

# Check if SSH key exists
if [ ! -f ~/.ssh/id_rsa.pub ]; then
    echo "SSH key not found. Generating new key..."
    ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""
fi

# Read SSH public key
SSH_PUBLIC_KEY=$(cat ~/.ssh/id_rsa.pub)

# Create cloud-init config
cat > scripts/cloud-init.yml << EOF
#cloud-config
users:
  - name: ubuntu
    sudo: ALL=(ALL) NOPASSWD:ALL
    shell: /bin/bash
    ssh_authorized_keys:
      - $SSH_PUBLIC_KEY

package_update: true
package_upgrade: true
EOF

# Create VM
echo "Creating VM in Yandex Cloud..."
yc compute instance create \
    --name $VM_NAME \
    --zone $ZONE \
    --network-interface subnet-name=$SUBNET_NAME,nat-ip-version=ipv4 \
    --cores $CORES \
    --memory $MEMORY \
    --create-boot-disk size=$DISK_SIZE,image-id=$IMAGE_ID \
    --platform standard-v3 \
    --metadata-from-file user-data=scripts/cloud-init.yml

# Get VM IP
VM_IP=$(yc compute instance get $VM_NAME --format json | jq -r '.network_interfaces[0].primary_v4_address.one_to_one_nat.address')

echo "VM created successfully!"
echo "IP address: $VM_IP"
echo "Please wait a few minutes for the VM to initialize..."
echo "Then you can connect using: ssh $VM_USER@$VM_IP"

# Wait for VM to be ready and cloud-init to finish
echo "Waiting for VM to be ready..."
sleep 120

# Function to check if VM is ready
check_vm_ready() {
    local max_attempts=10
    local attempt=1
    local sleep_time=10

    while [ $attempt -le $max_attempts ]; do
        if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no $VM_USER@$VM_IP "echo 'VM is ready'" &>/dev/null; then
            echo "VM is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: VM not ready yet, waiting ${sleep_time}s..."
        sleep $sleep_time
        attempt=$((attempt + 1))
    done

    echo "Failed to connect to VM after $max_attempts attempts"
    return 1
}

# Check if VM is ready
if ! check_vm_ready; then
    echo "Error: VM is not responding. Please check the VM status in Yandex Cloud console."
    exit 1
fi

# Run setup script on VM
echo "Running setup script on VM..."
scp scripts/setup-server.sh $VM_USER@$VM_IP:/tmp/
ssh $VM_USER@$VM_IP "bash /tmp/setup-server.sh"

# Verify Docker installation
echo "Verifying Docker installation..."
if ! ssh $VM_USER@$VM_IP "docker --version" &>/dev/null; then
    echo "Error: Docker installation failed"
    exit 1
fi

echo "Setup completed! Your VM is ready for deployment."
echo "Please add the following secrets to your GitHub repository:"
echo "SERVER_HOST: $VM_IP"
echo "SERVER_USER: $VM_USER"
echo "SERVER_SSH_KEY: (your private SSH key)"

# Calculate and display estimated monthly cost
echo -e "\nEstimated monthly cost breakdown:"
echo "VM (2 vCPU, 2GB RAM): ~7,000 RUB"
echo "Disk (30GB): ~300 RUB"
echo "Network (outgoing traffic): ~100 RUB"
echo "Total estimated cost: ~7,400 RUB/month"

# Display next steps
echo -e "\nNext steps:"
echo "1. Add the secrets to your GitHub repository"
echo "2. Copy docker-compose.yml to the server:"
echo "   scp docker-compose.yml $VM_USER@$VM_IP:/opt/mybudget/backend/"
echo "3. Configure GitHub Actions workflow"
echo "4. Test the deployment with a push to backend-ci branch" 