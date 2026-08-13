# Product Requirements Document (PRD) - TripLedger V1

## 1. Product Vision & Value Proposition
TripLedger is a collaborative group travel management platform designed to simplify expense tracking and trip settlements. V1 establishes the core trip foundation: secure, fast expense logging, automatic optimized settlement, and offline-first reliability.

---

## 2. User Personas
1. **The Organizer (Rahul, 28)**: Plans the logistics, keeps track of the budget, and initiates settlements. Needs clear admin tooling, easy invitations, and real-time budget status.
2. **The Passive Traveler (Suresh, 31)**: Reluctant to log details, forgets what they spent on. Needs extremely fast (<30s) entry, automatic equal splitting options, and clear reminders.
3. **The Budget-Conscious Traveler (Priya, 24)**: Worried about overspending and optional expenses (e.g., alcohol or shopping splits they didn't participate in). Needs precise custom split controls.

---

## 3. User Journey & Screen Flows

```
[Sign Up / Auth]
      ↓
[Dashboard: Empty State] ────(Join via Link)────► [Join Success Screen]
      ↓ (Create Trip)
[Create Trip Screen]
      ↓
[Invite Code / Link Generation] ──(Share link: WhatsApp/Email)
      ↓
[Trip Dashboard (Timeline & Budget)] ◄─── (Members Join)
      ↓
[Add/Edit Expense Form] ──(Split Customizer)
      ↓
[Expense Detail / Audit History]
      ↓
[Settlement / Balances Sheet] ──(Mark Paid)
      ↓
[Archive Trip]
```

---

## 4. Feature Prioritization (V1 MVP Scope)

### Must-Have
- **Authentication**: Email/Password + Google Sign-In placeholder interface.
- **Trip Lifecycle**: Create, update, view dashboard, and archive.
- **Dynamic Invite Links**: Deep link `https://tripledger.app/join/{inviteCode}` triggering auto-join behavior.
- **Budget Tracking**: Real-time progress bar (Spent vs. Remaining).
- **Expense Logging & Custom Splits**: Equal split, fixed amount split, and exclusion options.
- **Trip Timeline**: Daily groupings of expenses.
- **Expense Audit History**: History log (e.g., "Raj added...", "Rahul edited amount...").
- **Offline Sync & Room DB**: Cache actions locally, queue sync, execute when online.
- **Settlement Logic**: Transaction minimization engine.

### Out of Scope (V2/V3)
- Push Notifications (will use in-app refresh/sync).
- PDF Exports, Charts & Analytics.
- AI OCR / Voice input.
