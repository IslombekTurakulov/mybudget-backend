# MyBudget Backend API Documentation

## Base URL
```
http://51.250.65.154
```

## Authentication

All API endpoints except `/ping` require authentication using JWT tokens.

### Headers
```
Authorization: Bearer <jwt_token>
```

## Endpoints

### Health Check
```
GET /ping
```
Response: `pong`

### Authentication

#### Login
```
POST /auth/login
```
Request body:
```json
{
    "email": "string",
    "password": "string"
}
```
Response:
```json
{
    "token": "string",
    "user": {
        "id": "string",
        "username": "string",
        "email": "string"
    }
}
```

#### Request Registration Code
```
POST /auth/request-register-code
```
Request body:
```json
{
    "email": "string"
}
```

#### Request Password Reset Code
```
POST /auth/request-reset-password-code
```
Request body:
```json
{
    "email": "string"
}
```

#### Verify Email Registration
```
POST /auth/verify-email-registration
```
Request body:
```json
{
    "email": "string",
    "code": "string",
    "username": "string",
    "password": "string"
}
```

#### Verify Reset Code
```
POST /auth/verify-reset-code
```
Request body:
```json
{
    "email": "string",
    "code": "string"
}
```

#### Change Password
```
POST /auth/change-password
```
Request body:
```json
{
    "oldPassword": "string",
    "newPassword": "string"
}
```

#### Refresh Token
```
POST /auth/refresh-token
```
Request body:
```json
{
    "refreshToken": "string"
}
```

### Projects

#### Get User Projects
```
GET /projects
```

#### Create Project
```
POST /projects
```
Request body:
```json
{
    "name": "string",
    "description": "string"
}
```

#### Get Project by ID
```
GET /projects/{projectId}
```

#### Update Project
```
PUT /projects/{projectId}
```
Request body:
```json
{
    "name": "string",
    "description": "string"
}
```

#### Delete Project
```
DELETE /projects/{projectId}
```

#### Get Project Participants
```
GET /projects/{projectId}/participants
```

#### Change Participant Role
```
PUT /projects/{projectId}/role
```
Request body:
```json
{
    "userId": "string",
    "role": "OWNER | ADMIN | MEMBER"
}
```

### Transactions

#### Get Project Transactions
```
GET /projects/{projectId}/transactions
```

#### Get Transaction by ID
```
GET /projects/{projectId}/transactions/{transactionId}
```

#### Create Transaction
```
POST /projects/{projectId}/transactions
```
Request body:
```json
{
    "amount": "number",
    "description": "string",
    "category": "string",
    "type": "INCOME | EXPENSE",
    "date": "string (ISO date)"
}
```

#### Update Transaction
```
PUT /projects/{projectId}/transactions/{transactionId}
```
Request body:
```json
{
    "amount": "number",
    "description": "string",
    "category": "string",
    "type": "INCOME | EXPENSE",
    "date": "string (ISO date)"
}
```

#### Delete Transaction
```
DELETE /projects/{projectId}/transactions/{transactionId}
```

### Analytics

#### Get Overview Analytics
```
GET /analytics/overview
```
Query parameters:
- `fromDate`: number (timestamp)
- `toDate`: number (timestamp)
- `categories`: string[] (optional)
- `granularity`: string (optional, "DAY", "WEEK", "MONTH", "YEAR")

#### Get Project Analytics
```
GET /analytics/project/{projectId}
```
Query parameters:
- `fromDate`: number (timestamp)
- `toDate`: number (timestamp)
- `categories`: string[] (optional)
- `granularity`: string (optional, "DAY", "WEEK", "MONTH", "YEAR")

#### Export Analytics
```
GET /analytics/export
```
Query parameters:
- `format`: string (export format)
- `projectId`: string
- `fromDate`: number (timestamp)
- `toDate`: number (timestamp)
- `categories`: string[] (optional)
- `granularity`: string (optional, "DAY", "WEEK", "MONTH", "YEAR")

### Notifications

#### Get User Notifications
```
GET /notifications
```

#### Mark Notification as Read
```
PUT /notifications/{notificationId}/read
```

### Settings

#### Get User Settings
```
GET /settings
```

#### Update User Settings
```
PUT /settings
```
Request body:
```json
{
    "language": "string",
    "theme": "string",
    "notifications": {
        "email": boolean,
        "push": boolean
    }
}
```

### Device Tokens

#### Register Device
```
POST /devices/register
```
Request body:
```json
{
    "token": "string",
    "platform": "string",
    "language": "string"
}
```

## Error Responses

All error responses follow this format:
```json
{
    "timestamp": "string (ISO date)",
    "status": "number",
    "error": "string",
    "message": "string",
    "path": "string"
}
```

Common status codes:
- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 500: Internal Server Error

## Rate Limiting

API requests are limited to:
- 100 requests per minute for authenticated users
- 20 requests per minute for unauthenticated users

## Versioning

The current API version is v1. The version is included in the URL path.