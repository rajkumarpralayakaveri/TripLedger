# Domain-Driven Design (DDD) - TripLedger V1

This document specifies the business domain contexts, aggregates, entities, and validation policies.

---

## 1. Domain Contexts & Boundaries

```
                       ┌───────────────────────────────┐
                       │        Trip Context           │
                       │     (Trip, Members)           │
                       └───────────────┬───────────────┘
                                       │
                        Tracks events  │  Requires
                                       ▼  Validation
                       ┌───────────────────────────────┐
                       │     Financial Split Context   │
                       │   (Expenses, Splits, Config)  │
                       └───────────────┬───────────────┘
                                       │
                        Recalculates   │  Settles
                                       ▼  Balances
                       ┌───────────────────────────────┐
                       │      Settlement Context       │
                       │     (Balances, Debts)         │
                       └───────────────────────────────┘
```

---

## 2. Aggregates & Entities

### 2.1 Trip Aggregate (Root)
- **Entities**: `Trip`, `Member`.
- **Invariants**:
  - Base currency cannot change once the trip transitions to `ACTIVE`.
  - Must have at least one `OWNER`.

### 2.2 Expense Aggregate (Root)
- **Entities**: `Expense`, `SplitDetail`.
- **Invariants**:
  - The sum of splits must equal the total expense `Money` amount.

### 2.3 Settlement Aggregate (Root)
- **Entities**: `SettlementEntry`, `SettlementBatch`.

### 2.4 Receipt Aggregate (Root)
- **Entities**: `Receipt`.

---

## 3. Value Objects

### 3.1 Money
Encapsulates arithmetic operations to prevent rounding errors:
```kotlin
data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return Money(amount.add(other.amount), currency)
    }
}
```

---

## 4. Domain Services
- **SettlementService**: Calculates optimal transaction paths from net balances.
- **SplitCalculationService**: Generates splits cleanly across decimal divisions.
- **TripInvitePolicy**: Validates invite token rules.

---

## 5. Domain Policies
- **Outstanding Settlement Lock Policy**: Members cannot leave a trip if their net balance is non-zero.
- **Smart Settlement Lock Policy**: Edits to expenses mark settlements stale and prompt recalculation.
- **Trip Status Transitions**: Trips cannot be archived while outstanding balances exist.
