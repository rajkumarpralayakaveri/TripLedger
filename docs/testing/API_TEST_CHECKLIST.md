# TripLedger API Verification Checklist

This checklist is used to manually verify all API endpoints using Swagger UI before a Release Candidate build is tagged.

---

## 1. Authentication Endpoints

| Endpoint | Method | Security | Expected Success | Status |
|---|---|---|---|---|
| `/api/v1/auth/register` | `POST` | Public | `201 Created` | Pass |
| `/api/v1/auth/login` | `POST` | Public | `200 OK` | Pass |
| `/api/v1/auth/refresh` | `POST` | Public | `200 OK` | Pass |
| `/api/v1/auth/logout` | `POST` | Public | `200 OK` / `204 No Content` | Pass |
| `/api/v1/users/me` | `GET` | Bearer JWT | `200 OK` | Pass |

---

## 2. Workspace (Trip) Endpoints

| Endpoint | Method | Security | Expected Success | Status |
|---|---|---|---|---|
| `/api/v1/workspaces` | `POST` | Bearer JWT | `201 Created` | Pass |
| `/api/v1/workspaces` | `GET` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}` | `GET` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}` | `PUT` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/invite` | `POST` | Bearer JWT | `200 OK` (Token DTO) | Pass |
| `/api/v1/workspaces/join` | `POST` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/archive` | `POST` | Bearer JWT | `200 OK` | Pass |

---

## 3. Contribution Endpoints

| Endpoint | Method | Security | Expected Success | Status |
|---|---|---|---|---|
| `/api/v1/workspaces/{id}/financial-summary` | `GET` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/contributions` | `POST` | Bearer JWT | `201 Created` | Pass |
| `/api/v1/workspaces/{id}/contributions/adjust` | `POST` | Bearer JWT | `201 Created` | Pass |

---

## 4. Expense Endpoints

| Endpoint | Method | Security | Expected Success | Status |
|---|---|---|---|---|
| `/api/v1/workspaces/{id}/expenses` | `POST` | Bearer JWT | `201 Created` | Pass |
| `/api/v1/workspaces/{id}/expenses/{expId}`| `PUT` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/expenses/{expId}`| `DELETE`| Bearer JWT | `200/204 OK` (Soft Delete) | Pass |
| `/api/v1/workspaces/{id}/expenses` | `GET` | Bearer JWT | `200 OK` (Timeline view) | Pass |
| `/api/v1/workspaces/{id}/activities` | `GET` | Bearer JWT | `200 OK` (Activity feed) | Pass |

---

## 5. Settlement Endpoints

| Endpoint | Method | Security | Expected Success | Status |
|---|---|---|---|---|
| `/api/v1/workspaces/{id}/balances` | `GET` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/settlements/plan` | `GET` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/settlements/{transferId}/confirm`| `POST` | Bearer JWT | `200 OK` | Pass |
| `/api/v1/workspaces/{id}/settlements/history` | `GET` | Bearer JWT | `200 OK` | Pass |
