# Sahm Food POS — Architecture Overview

## 1. Project Structure

```
kmm_pos/
├── shared/                          ← KMP shared module (90 % of business logic)
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── domain/              ← INNER ring — pure Kotlin, zero platform deps
│       │   │   ├── model/           Product, CartItem, Order, Transaction, SyncQueueItem
│       │   │   ├── repository/      Interfaces: ProductRepository, OrderRepository …
│       │   │   └── usecase/         CartUseCases, PaymentUseCases, ProductUseCases
│       │   ├── data/                ← MIDDLE ring — implements domain contracts
│       │   │   ├── repository/      Impl classes backed by SQLDelight
│       │   │   ├── hardware/        MockReceiptPrinter, MockBarcodeScanner, MockPaymentTerminal
│       │   │   └── sync/            SyncService (outbox pattern), RemoteApi
│       │   ├── presentation/        ← OUTER shared ring — KMP ViewModels with StateFlow
│       │   │   ├── cart/            CartViewModel, CartUiState
│       │   │   ├── payment/         PaymentViewModel, PaymentUiState
│       │   │   ├── transaction/     TransactionViewModel, TransactionUiState
│       │   │   └── order/           OrderListViewModel, OrderListUiState
│       │   ├── di/                  sharedModule() — Koin module for all layers
│       │   └── sqldelight/          .sq schema files → auto-generated typesafe DAOs
│       ├── androidMain/kotlin/      Android-specific drivers (SQLite, Ktor OkHttp)
│       ├── iosMain/kotlin/          iOS-specific drivers (NativeSQLite, Ktor Darwin)
│       └── commonTest/kotlin/       TDD unit + integration tests
│
├── androidApp/                      ← Android host application
│   └── src/main/kotlin/
│       ├── PosApplication.kt        Koin init, SyncService start
│       ├── MainActivity.kt          Single-activity, Compose host
│       ├── di/                      AndroidX ViewModel wrappers (bridge KMP↔lifecycle)
│       └── ui/
│           ├── theme/               Material 3 colour scheme
│           ├── PosApp.kt            NavHost + bottom navigation
│           ├── cart/                CartScreen (Jetpack Compose)
│           ├── payment/             PaymentScreen
│           ├── transaction/         TransactionScreen
│           ├── order/               OrderListScreen
│           └── components/          Reusable composables
│
└── iosApp/                          ← Swift/SwiftUI iOS shell (framework consumer)
```

---

## 2. Architecture: Clean Architecture + SOLID

```
┌──────────────────────────────────────────────┐
│  Presentation Layer  (ViewModels / Compose)  │  ← Depends on domain interfaces only
├──────────────────────────────────────────────┤
│  Domain Layer  (Use Cases + Entities)        │  ← Pure Kotlin, no framework imports
├──────────────────────────────────────────────┤
│  Data Layer  (Repositories + SQLDelight)     │  ← Implements domain interfaces
└──────────────────────────────────────────────┘
```

### Why this design?

| SOLID Principle | How it's applied |
|---|---|
| **S — Single Responsibility** | Each use case class does exactly one thing (`AddItemToCartUseCase`, `ProcessPaymentUseCase` …) |
| **O — Open/Closed** | `ReceiptPrinter`, `BarcodeScanner`, `PaymentTerminal` are interfaces; new implementations never touch existing code |
| **L — Liskov Substitution** | `MockReceiptPrinter` is fully substitutable for any real thermal printer |
| **I — Interface Segregation** | `ProductRepository`, `OrderRepository`, `TransactionRepository`, `SyncRepository` are separate contracts |
| **D — Dependency Inversion** | Domain layer defines interfaces; data layer depends inward, not outward |

---

## 3. State Management

Each screen has a corresponding **ViewModel** that exposes a single immutable `UiState` via `StateFlow<T>`. Events flow in as method calls; updates flow out as new state snapshots.

```
User action
    │
    ▼
ViewModel.method()
    │  suspends / launches coroutine
    ▼
UseCase.invoke()       ← pure business logic, returns Result<T>
    │
    ▼
Repository.save()      ← persists locally
    │
    ▼
StateFlow emits new UiState
    │
    ▼
Compose recomposition  ← 0 manual invalidation
```

**Android bridge**: KMP ViewModels take a `CoroutineScope`. Android-specific `*AndroidViewModel` wrappers (`CartAndroidViewModel`, etc.) extend `AndroidX ViewModel` and pass `viewModelScope`, giving automatic lifecycle-aware cancellation.

---

## 4. Offline-First Strategy

### Local-first writes

Every mutation (create order, add item, process payment) writes to **SQLite via SQLDelight** before any network call. The app is fully operational with zero connectivity.

### Outbox / Sync Queue

When a transaction is created locally, a `SyncQueueItem` is enqueued in `SyncQueueEntity`. The `SyncService` drains this queue in the background:

```
┌──────────┐    enqueue    ┌──────────────┐   upload   ┌─────────────┐
│  Domain  │ ──────────── ▶│  SyncQueue   │ ──────────▶│  Remote API │
│  UseCase │               │  (SQLite)    │            │  (Ktor)     │
└──────────┘               └──────────────┘            └─────────────┘
                                   ▲
                            retry on failure
```

### Retry & Back-off

| Attempt | Delay   |
|---------|---------|
| 1       | 2 s     |
| 2       | 4 s     |
| 3       | 8 s     |
| 4       | 16 s    |
| 5       | 32 s    |
| > 5     | ABANDONED (logged) |

### Conflict Resolution

Strategy: **last-write-wins with server timestamp**.  
The server accepts a `paidAt` / `createdAt` epoch field. If the server has a newer version for the same `orderId`, it rejects the upload and returns `409 Conflict`. The client marks the item `ABANDONED` and flags the local record for a pull refresh on next online session.

---

## 5. Hardware Simulation

| Hardware | Interface | Mock | Real Swap |
|---|---|---|---|
| Receipt Printer | `ReceiptPrinter` | `MockReceiptPrinter` — formats receipt string + 400 ms delay | ESC/POS over USB/BT |
| Barcode Scanner | `BarcodeScanner` | `MockBarcodeScanner` — emits from preset barcode list every 3 s | Camera ML Kit / USB HID |
| Payment Terminal | `PaymentTerminal` | `MockPaymentTerminal` — approves after 1 s | NETS / Verifone SDK |

All three are injected via Koin. Swapping from mock to real requires only a single line change in the DI module.

---

## 6. Sync Design (Scalable)

### Current: Outbox queue (SQLite → REST)

Transactions and orders are enqueued after local commit. `SyncService` polls every 30 s and retries with exponential back-off.

### Production-ready additions (given more time)

1. **WorkManager** (Android) / `BGTaskScheduler` (iOS) — sync even when app is backgrounded.  
2. **Change-data timestamps** — every entity carries `updatedAt`; the server rejects stale writes.  
3. **Pull-based reconciliation** — on each app open, fetch server state since `lastSyncAt`; merge with local.  
4. **Vector clock / CRDT** for cart merging across devices on the same order (multi-cashier support).

---

## 7. Multi-Branch Scalability

For a multi-branch restaurant chain (e.g. 100+ Sahm Food locations):

| Concern | Solution |
|---|---|
| **Branch isolation** | Each device carries `branchId` and `cashierId` on every record |
| **Central dashboard** | Server aggregates transactions by `branchId` for analytics |
| **Menu sync** | Products are pulled from server on app start and cached locally |
| **Shift reporting** | `TransactionRepository` queries by `cashierId + paidAt range` |
| **Receipt numbering** | Server-assigned sequential receipt numbers on sync confirmation |
| **Offline capability** | Full offline for up to 7 days; forced sync on re-connection |

---

## 8. Trade-offs & What Would Improve with More Time

| Simplified | Production alternative |
|---|---|
| `MockRemoteApi` always succeeds | Real Ktor HTTP client with TLS + JWT auth |
| In-memory `kotlinx.datetime.Clock.System.now()` | NTP-verified timestamp service |
| Single-device design | Multi-device order management with shared order state |
| No authentication | Cashier PIN / biometric login with role-based permissions |
| No analytics | Firebase Analytics / custom event logging |
| Mock barcode scanner | ML Kit Barcode Scanning on camera preview |
| Single currency (SAR) | Multi-currency support with exchange rates |
| Flat product list | Category tree + modifier groups (e.g., size, extra shots) |
