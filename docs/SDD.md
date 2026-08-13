# System Design Document (SDD) - TripLedger V1

This document outlines the API designs, database structures, performance targets, and system flows for TripLedger.

---

## 1. Performance Targets & SLA

| Metric | Target |
|---|---|
| App Cold Start | < 2 seconds |
| Add Expense Operation | < 500 ms |
| Open Trip Details View | < 1 second |
| Settlement Generation | < 2 seconds for 500 expenses |
| Offline Sync Reconnect | < 10 seconds |
| Backend API Availability | 99.9% |

---

## 2. API Architecture & Routing Conventions

### Versioning
All backend REST resources must use explicit version routing prefixes:
`https://api.tripledger.app/api/v1/...`

### Centralized Exception Envelope
```json
{
  "success": false,
  "error": {
    "code": "SETTLEMENT_STALE",
    "message": "The settlements need to be recalculated due to a modified expense."
  }
}
```

---

## 3. Database Schema (PostgreSQL)

### 3.1 Trip & Financial Precision
```sql
CREATE TABLE trips (
    id VARCHAR(36) PRIMARY KEY, -- UUID v7 (Time-ordered)
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PLANNING', 'ACTIVE', 'COMPLETED', 'ARCHIVED'
    budget DECIMAL(19, 4),
    base_currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expenses (
    id VARCHAR(36) PRIMARY KEY, -- UUID v7
    trip_id VARCHAR(36) REFERENCES trips(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    paid_by_id VARCHAR(36) REFERENCES users(id),
    category_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE categories (
    id VARCHAR(36) PRIMARY KEY, -- UUID v7
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(10),
    is_system BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    trip_id VARCHAR(36) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE TABLE receipts (
    id VARCHAR(36) PRIMARY KEY, -- UUID v7
    expense_id VARCHAR(36) REFERENCES expenses(id) ON DELETE SET NULL,
    file_path_local VARCHAR(255),
    file_path_remote VARCHAR(255),
    ocr_data JSONB,
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);
```

### 3.2 Activity Logs Table
Instead of event-sourcing the main state, activities are written as simple flat records for history viewing:
```sql
CREATE TABLE activity_logs (
    id VARCHAR(36) PRIMARY KEY, -- UUID v7
    trip_id VARCHAR(36) REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL, -- 'EXPENSE_ADDED', 'EXPENSE_EDITED', etc.
    description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```
