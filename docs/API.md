# API Specification - TripLedger V1

This document specifies the REST API endpoints, payload contracts, and error responses for TripLedger V1.

All endpoints are hosted under the base path `/api/v1`.

---

## 1. Authentication Endpoints

### 1.1 Email Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "user": {
      "id": "usr_0190ed02-0e9e-71bd-9bc2-3c1e5cbde1a4",
      "name": "Raj Kumar",
      "email": "user@example.com",
      "avatarUrl": "https://tripledger.app/avatars/usr_1.png"
    }
  }
}
```
- **Error Response (401 Unauthorized)**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password provided."
  }
}
```

### 1.2 Google Sign-In Login
- **Endpoint**: `POST /api/v1/auth/google`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "idToken": "google_id_token_jwt_from_android_sdk"
}
```
- **Success Response (200 OK)**: Same as `POST /api/v1/auth/login`.

### 1.3 Refresh Access Token
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "new_eyJhbGciOi...",
    "refreshToken": "new_eyJhbGciOi..."
  }
}
```

### 1.4 Logout
- **Endpoint**: `POST /api/v1/auth/logout`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Logged out successfully."
}
```

### 1.5 Get Current Profile
- **Endpoint**: `GET /api/v1/users/me`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": "usr_0190ed02-0e9e-71bd-9bc2-3c1e5cbde1a4",
    "name": "Raj Kumar",
    "email": "user@example.com",
    "avatarUrl": "https://tripledger.app/avatars/usr_1.png"
  }
}
```
