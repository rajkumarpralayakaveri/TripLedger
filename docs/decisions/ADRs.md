# Architectural Decision Records (ADRs) - TripLedger

This document catalogs the fundamental architectural decisions made during the planning phase of TripLedger.

---

## ADR-001: Choice of Spring Boot (Backend Framework)
- **Status**: Approved
- **Context**: The backend requires high reliability, secure JWT authentication, transactional database support (JPA/PostgreSQL), and scalability.
- **Decision**: Use Spring Boot with Kotlin/Java.
- **Consequences**: Easy dependency injection, robust JPA support, and clean boundaries separating presentation controllers from core domain services.

---

## ADR-002: Use of UUID v7 as Primary Identity Keys
- **Status**: Approved
- **Context**: Tripledger is offline-first. Mobile clients need to generate entity IDs offline without server roundtrips, but standard UUID v4 indexes perform poorly in database b-trees due to randomness.
- **Decision**: Standardize on UUID v7 for all table keys.
- **Consequences**: Client-side generation is fully supported, and entries are naturally time-ordered, improving database index clustering and write throughput.

---

## ADR-003: SQLite / Room Local Database (Android)
- **Status**: Approved
- **Context**: The application must run offline. Local modifications must persist when network connection is absent.
- **Decision**: Implement Room DB on Android.
- **Consequences**: Standardized SQLite integrations, reactive Kotlin Flow query streams, and clean synchronization state markers (`DRAFT`, `PENDING_SYNC`, `SYNCED`).

---

## ADR-004: OAuth 2.0 Google Sign-In & Passwords
- **Status**: Approved
- **Context**: Password-only flows introduce signup friction. SMS OTP creates high operational run-costs.
- **Decision**: Prioritize Google Sign-In and standard Email/Password authentication.
- **Consequences**: Streamlined user onboarding. Allows phone sign-in as a potential extension in future versions.

---

## ADR-005: Offline-First Sync Queue
- **Status**: Approved
- **Context**: Travel often occurs in areas with weak signals. App data writes must not fail when offline.
- **Decision**: Record all user mutations in a local database event queue, and run a `WorkManager` synchronization client when network connectivity is detected.
- **Consequences**: Zero user friction during network outages. Conflicts are resolved on the server using physical Lamport offsets and Last-Write-Wins (LWW) rules.

---

## ADR-006: Temporary Deep-Link Invitation Access
- **Status**: Approved
- **Context**: Static invite codes are easily leaked.
- **Decision**: Generate short-lived deep-link invite tokens (`/join/{token}`).
- **Consequences**: Increased system security. The invite token can be configured to expire after 7 days or restricted to single-use.
