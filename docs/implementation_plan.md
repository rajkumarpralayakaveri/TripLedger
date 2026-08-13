# TripLedger MVP (Version 1) Implementation Plan

This document maps out the five product milestones for implementing TripLedger V1.

---

## 1. Product Milestones

### Milestone 1 — Foundation
- **Tasks**: Email Sign-In, Google Sign-In interface layout, Trip creation API endpoints, Member joining via deep links.

### Milestone 2 — Financial Core
- **Tasks**: Expense logging using the `Money` Value Object, Timeline projection diary layout grouped chronologically, database-configured reference categories.

### Milestone 3 — Offline Reliability
- **Tasks**: Room entities caching locally, Event Sourcing Sync queue matching UUID v7, Idempotency keys (`operationId`) checked on API endpoints.

### Milestone 4 — Settlement
- **Tasks**: Settlement calculation domain services on the backend, transaction matching engines, settlement lock checking (making settlements stale if expenses are edited).

### Milestone 5 — Polish
- **Tasks**: Decoupled Receipt uploads, soft deletes, Structured observability logs integration, activity feed timeline construction, empty/error state illustrations.

---

## 2. Verification Plan

### Milestone 1 Verification
- Trigger deep link mapping:
  `adb shell am start -W -a android.intent.action.VIEW -d "https://tripledger.app/join/GOA72X" com.rkdevstudios.tripledger`

### Milestone 2 Verification
- Unit test rounding parameters inside the `Money` value object across unequal split allocations.

### Milestone 3 Verification
- Mock physical disconnects, write 3 local transactions, reconnect, and verify that `operationId` prevents duplicate execution on the server.
