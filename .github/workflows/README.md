# GitHub Actions Workflows

This directory contains GitHub Actions workflows for automating the build, test, and deployment processes of the MyBudget Backend application.

## Available Workflows

### Quick Deploy (`quick-deploy.yml`)

This workflow provides a quick way to deploy the application to the production server.

#### Features
- Manual trigger with customizable image tag
- Automatic secret validation
- Docker Swarm deployment
- Health checks and monitoring
- Log collection and artifact upload

#### Usage
1. Go to the Actions tab in GitHub
2. Select "Quick Deploy" workflow
3. Click "Run workflow"
4. (Optional) Enter a custom image tag
5. Click "Run workflow"

#### Required Secrets
- `SERVER_HOST`: Production server hostname
- `SERVER_USER`: SSH username
- `SERVER_SSH_KEY`: SSH private key
- `PG_USER`: PostgreSQL username
- `PG_PASSWORD`: PostgreSQL password
- `PG_DATABASE`: PostgreSQL database name
- `JWT_SECRET`: JWT signing key
- `GITHUB_TOKEN`: GitHub token for container registry access

## Workflow Structure

### Quick Deploy Workflow

1. **Validation**
   - Checkout repository
   - Validate required secrets
   - Verify server connectivity

2. **Deployment**
   - Initialize Docker Swarm
   - Clean up existing resources
   - Create networks and secrets
   - Deploy services (database, traefik, backend)
   - Verify service health

3. **Monitoring**
   - Collect service logs
   - Upload logs as artifacts
   - Display deployment status

## Best Practices

1. **Security**
   - All sensitive data is stored as GitHub secrets
   - SSH keys are used for secure server access
   - Docker secrets are used for service configuration

2. **Reliability**
   - Health checks ensure service availability
   - Automatic rollback on deployment failure
   - Comprehensive logging for troubleshooting

3. **Maintenance**
   - Regular cleanup of unused resources
   - Log rotation and management
   - Resource limits and monitoring

## Troubleshooting

### Common Issues

1. **Deployment Fails**
   - Check server connectivity
   - Verify all required secrets are set
   - Review service logs in artifacts

2. **Services Not Starting**
   - Check Docker Swarm status
   - Verify network connectivity
   - Review service-specific logs

3. **Log Collection Fails**
   - Verify SSH access
   - Check file permissions
   - Review service status

### Getting Help

If you encounter issues:
1. Check the workflow logs in GitHub Actions
2. Review the collected logs in artifacts
3. Verify server status and connectivity
4. Contact the development team 