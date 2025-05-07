#!/bin/bash

# Exit on error
set -e

VM_NAME="mybudget-backend"
ZONE="ru-central1-a"

echo "Stopping and deleting VM $VM_NAME..."
yc compute instance stop $VM_NAME
yc compute instance delete $VM_NAME

echo "VM $VM_NAME has been deleted successfully!" 