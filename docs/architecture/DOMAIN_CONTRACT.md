# Domain Contract Document — TripLedger Financial Core

This document outlines the strict architectural rules, invariants, and domain contracts governing the TripLedger core financial domains.

---

## 1. Core Financial Invariants

The following rules represent system-wide invariants that must be validated at the domain layer and tested continuously:

| Invariant | Description | Enforced By |
|---|---|---|
| **Non-Negative Money** | Transaction amounts (Expenses, Contributions) must be strictly positive (> 0) unless expressing negative correction adjustments. | `Money.java`, `ExpenseService.java` |
| **Workspace Base Currency** | A workspace has exactly one base currency. All expenses and transactions inside a workspace must match its `baseCurrency`. | `ExpenseService.java`, `ContributionService.java` |
| **Net Zero Balances** | The sum of all member balances in any workspace must equal zero exactly (`Sum(Balances) == 0`). | `SettlementEngine.java` |
| **Plan Immutability** | A `SettlementPlan` is a read-only recommendation and must never mutate state or modify database balances. | `SettlementService.java` |
| **Atomic Repayments** | Only confirmed `SettlementTransaction` records can mutate contribution ledgers. Every confirmed transaction writes exactly once and publishes exactly one event. | `SettlementService.java` |
| **Derivability of Summaries** | Financial snapshots and member summaries must be derived on-the-fly from ledger history, never stored independently. | `ContributionController.java` |
| **Soft-Delete Exclusion** | Soft-deleted expenses (status = `DELETED`) must be excluded from all calculations, summaries, and plans. | `ExpenseRepository.java`, `SettlementService.java` |
| **Archived Immutability** | Workspaces with status `ARCHIVED` are read-only; no new expenses, contributions, or settlements may be confirmed. | `SettlementService.java`, `ExpenseService.java` |

---

## 2. Domain Events Registry

All cross-module interaction must trigger asynchronous or commit-phase decoupled updates using these events:

1. **`ExpenseCreatedEvent`**: Fired when a new expense is successfully saved. Recalculates planned contribution offsets.
2. **`ExpenseUpdatedEvent`**: Fired when an expense is edited. Generates an adjustment entry negating the old value and adding the new value.
3. **`ExpenseDeletedEvent`**: Fired when an expense is soft-deleted. Generates a negative adjustment entry to correct the balance sheet.
4. **`SettlementConfirmedEvent`**: Fired when a settlement payment is confirmed. Records a cash contribution for the debtor and an adjustment for the creditor.
5. **`WorkspaceArchivedEvent`**: Fired when a workspace transitions to `ARCHIVED` status. Blocks all pending mutations.

---

## 3. Strict Module Dependencies

To maintain loose coupling, modules are prohibited from querying other modules' repositories directly. All cross-module operations must go through **Domain Services**, **Domain Events**, or **Immutable State Snapshots** (e.g. `WorkspaceFinancialState`).

```
  [Identity / Auth]
          │
          ▼
     [Workspace]
          │
  ┌───────┼───────┐
  ▼       ▼       ▼
[Contr] [Exp] [Activity]
  │       │
  └─┬─────┘
    ▼
[Settlement]
```
- **Authentication**: Core authentication context.
- **Workspace**: Defines membership roles (`OWNER`, `ADMIN`, `MEMBER`) and lifecycle states.
- **Contribution Ledger**: Manages actual trip funding and adjustment logs.
- **Expense Ledger**: Tracks expense items and split allocations.
- **Settlement Engine**: Consumes snapshot states from Contribution and Expense modules to compute minimized cash transfers.
