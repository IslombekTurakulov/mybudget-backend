# MyBudget Backend (Мой бюджет)

Backend service for the MyBudget application, built with Kotlin and Ktor. This service provides an API for managing finances.

## Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Development](#-development)
- [Deployment](#-deployment)
- [Server Management](#-server-management)
- [API Documentation](#-api-documentation)
- [Contributing](#-contributing)

## 🚀 Features

### Core Features
- **User Management**
  - Secure registration and login
  - JWT-based authentication
  - Firebase integration

- **Transaction Management**
  - Create, read, update, and delete transactions
  - Categorize transactions
  - Filter and search transactions

- **Analytics**
  - Project analytics
  - Overview analytics 

- **Firebase cloud messaging notification with ru/en localization**


### Technical Features
- RESTful API design
- Swagger UI documentation
- Health check endpoints
- Docker containerization
- CI/CD pipeline with GitHub Actions

## Tech Stack

### Backend
- **Language**: Kotlin 1.9.22
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 17
- **Authentication**: JWT + Firebase
- **Container**: Docker
- **CI/CD**: GitHub Actions
- **Reverse Proxy**: Traefik v2.11

## Project Structure

```
src/main/kotlin/ru/iuturakulov/mybudgetbackend/
├── Application.kt           # Application entry point
├── config/                 # Configuration files
├── controller/            # API controllers
├── database/             # Database configuration
├── di/                   # Dependency injection
├── entities/            # Database entities
├── extensions/         # Kotlin extensions
├── models/            # Data models
├── plugins/          # Ktor plugins
├── repositories/    # Data repositories
├── routing/        # API routes
├── services/      # Business logic
└── utils/        # Utility functions
```

## Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **JDK 17**
  ```bash
  # Ubuntu/Debian
  sudo apt install openjdk-17-jdk
  
  # macOS
  brew install openjdk@17
  ```

- **Docker & Docker Compose**
  ```bash
  # Ubuntu/Debian
  sudo apt install docker.io docker-compose
  
  # macOS
  brew install docker docker-compose
  ```

### Required Accounts
- GitHub account (for repository access)
- Firebase project (for authentication)

## 🔧 Installation

### 1. Clone the Repository
```bash
git clone https://github.com/IslombekTurakulov/mybudget-backend.git
cd mybudget-backend
```

### 2. Set Up Environment Variables
Create a `.env` file in the project root:
```env
# Database Configuration
PG_USER=your_db_user
PG_DATABASE=your_db_name
PG_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your_jwt_secret

# Let's Encrypt Configuration
LETSENCRYPT_EMAIL=your_email@example.com
```

### 3. Build the Project
```bash
./gradlew build
```

## Development

### Running Locally
1. Start the database:
```bash
docker-compose up -d database
```

2. Run the application:
```bash
./gradlew run
```

### Running Tests
```bash
# Run all tests
./gradlew test
```

## 🐳 Deployment

### Docker Deployment
1. Build the image:
```bash
docker build -t ghcr.io/islombekturakulov/mybudget-backend:latest .
```

2. Run with Docker Compose:
```bash
docker-compose up -d
```

### Production Deployment
The project uses GitHub Actions for automated deployment:

1. **Manual Deployment**
   - Go to GitHub Actions tab
   - Select "Backend CI/CD" workflow
   - Click "Run workflow"
   - Select "Deploy to server" option

2. **Automatic Deployment**
   - Create a new version tag:
   ```bash
   git tag v0.0.1
   git push origin v0.0.1
   ```

## 📊 Deployment Status

### Latest Release
[![Latest Release](https://img.shields.io/github/v/release/IslombekTurakulov/mybudget-backend?include_prereleases&sort=semver)](https://github.com/IslombekTurakulov/mybudget-backend/releases/latest)

## 📚 API Documentation

API documentation is available at `/swagger-ui` when the application is running.

## 🛠 Server Management

### Basic Commands

#### Service Status
```bash
# List all services
docker service ls

# Check specific service status
docker service ps backend_backend
docker service ps backend_database
docker service ps backend_traefik
```

#### Logs
```bash
# View backend logs
docker service logs backend_backend

# View database logs
docker service logs backend_database

# View Traefik logs
docker service logs backend_traefik

# Follow logs in real-time
docker service logs -f backend_backend
```

#### Networks
```bash
# List all networks
docker network ls

# Inspect specific network
docker network inspect backend_backend
docker network inspect backend_public
```

#### Volumes
```bash
# List all volumes
docker volume ls

# Inspect specific volume
docker volume inspect backend_pg_data
docker volume inspect backend_letsencrypt
```

#### Containers
```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a
```

### Health Checks

#### Application Health
```bash
# Check application health
curl http://localhost:8080/ping

# Check Traefik dashboard
curl http://localhost:8080/dashboard/
```

#### Port Status
```bash
# Check open ports
netstat -tulpn | grep -E '80|443|8080'
```

### Service Management

#### Restart Services
```bash
# Restart backend
docker service update --force backend_backend

# Restart database
docker service update --force backend_database

# Restart Traefik
docker service update --force backend_traefik
```

#### Scale Services
```bash
# Scale backend to 3 replicas
docker service scale backend_backend=3

# Scale backend to 1 replica
docker service scale backend_backend=1
```

#### Full Stack Update
```bash
# Remove stack
docker stack rm backend

# Wait for cleanup
sleep 10

# Deploy stack
docker stack deploy -c docker-compose.yml backend
```

### Resource Monitoring

#### System Resources
```bash
# Monitor resource usage
docker stats

# System information
docker info
```

### Troubleshooting

#### Common Issues

1. **Service Not Starting**
   ```bash
   # Check service logs
   docker service logs backend_backend
   
   # Check service status
   docker service ps backend_backend
   
   # Check container logs
   docker ps
   docker logs <container_id>
   ```

2. **Database Issues**
   ```bash
   # Check database logs
   docker service logs backend_database
   
   # Check database connection
   docker exec -it $(docker ps -q -f name=backend_database) psql -U $PG_USER -d $PG_DATABASE
   ```

3. **Network Issues**
   ```bash
   # Check network connectivity
   docker network inspect backend_backend
   
   # Check Traefik configuration
   docker service logs backend_traefik | grep "configuration"
   ```

4. **Port Conflicts**
   ```bash
   # Check port usage
   netstat -tulpn | grep -E '80|443|8080'
   
   # Check Traefik logs
   docker service logs backend_traefik
   ```

#### Recovery Steps

1. **Service Recovery**
   ```bash
   # Force update service
   docker service update --force backend_backend
   
   # Check service status
   docker service ps backend_backend
   ```

2. **Stack Recovery**
   ```bash
   # Remove stack
   docker stack rm backend
   
   # Clean up resources
   docker container prune -f
   docker network prune -f
   docker volume prune -f
   
   # Redeploy stack
   docker stack deploy -c docker-compose.yml backend
   ```

3. **Database Recovery**
   ```bash
   # Backup database
   docker exec -it $(docker ps -q -f name=backend_database) pg_dump -U $PG_USER $PG_DATABASE > backup.sql
   
   # Restore database
   docker exec -i $(docker ps -q -f name=backend_database) psql -U $PG_USER -d $PG_DATABASE < backup.sql
   ```

### Security

#### Update Secrets
```bash
# Update JWT secret
echo "new_jwt_secret" > secrets/app_jwt_secret.txt
chmod 600 secrets/app_jwt_secret.txt
docker service update --secret-rm app_secret --secret-add source=app_secret,target=/run/secrets/app_secret backend_backend

# Update database password
echo "new_db_password" > secrets/pg_password.txt
chmod 600 secrets/pg_password.txt
docker service update --secret-rm pg_password --secret-add source=pg_password,target=/run/secrets/pg_password backend_database
```

#### SSL/TLS
```bash
# Check SSL certificate status
docker service logs backend_traefik | grep "certificate"

# Force certificate renewal
docker service update --force backend_traefik
```
