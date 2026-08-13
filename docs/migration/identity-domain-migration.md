# Identity Domain Migration Reference

This document serves as a design archive detailing the original entity schemas, annotations, fields, and repository methods mapped from the initial Kotlin design to the canonical Java + Spring Boot backend.

---

## 1. Domain Model: User Entity (`User.java`)

### Annotations
- `@Entity`
- `@Table(name = "users")`

### Attributes & Constraints
- **`id`** (`String`): 
  - Represents a time-ordered UUID v7 string identifier.
  - `@Id`
  - `@Column(length = 36)`
- **`name`** (`String`):
  - `@Column(nullable = false, length = 100)`
- **`email`** (`String`):
  - `@Column(nullable = false, unique = true, length = 150)`
- **`passwordHash`** (`String`):
  - `@Column(name = "password_hash", nullable = false)`
- **`avatarUrl`** (`String`, nullable):
  - `@Column(name = "avatar_url")`

---

## 2. Repository Layer: UserRepository (`UserRepository.java`)

### Interface Signature
- `public interface UserRepository extends JpaRepository<User, String>`

### Custom Database Query Methods
- `Optional<User> findByEmail(String email);`
