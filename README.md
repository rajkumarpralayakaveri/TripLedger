# TripLedger

TripLedger is a collaborative group travel management platform designed to simplify expense tracking and trip settlements.

## Repository Structure
- **android-app/**: The native Kotlin/Jetpack Compose Android client.
- **backend/**: Spring Boot service handling authentication, calculation APIs, and persistence.
- **web/**: React / TypeScript dashboard portal.
- **docs/**: Product Requirements, System Design, and Architecture Decision Records (ADRs).
- **infrastructure/**: Docker and deployment configurations.

## Git Workflow
We use a structured branch strategy:
- `main` is production-ready, locked from direct commits.
- `develop` holds the active milestones updates.
- Feature branches (`feature/auth`, `feature/trips`, etc.) must target `develop` via pull requests.

## Coding Standards
- Commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) convention.
- Formatting checks: Kotlin (ktlint/detekt), Java (Spotless), Web (ESLint/Prettier).
