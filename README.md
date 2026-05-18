# Sahm Food POS — Kotlin Multiplatform

A production-quality offline-first Point-of-Sale system built with **Kotlin Multiplatform**, **Jetpack Compose**, **SQLDelight**, and **Clean Architecture**.

---

## Features

| Feature | Status |
|---|---|
| Create & manage orders | ✅ |
| Add / remove / update cart items | ✅ |
| Tax (15% VAT) & discount calculation | ✅ |
| Cash / Card / Mobile Wallet payment flow | ✅ |
| Receipt generation (simulated printer) | ✅ |
| Barcode scanning (mock scanner + manual input) | ✅ |
| Card terminal simulation | ✅ |
| SQLite offline storage (SQLDelight) | ✅ |
| Outbox sync queue with exponential back-off | ✅ |
| Transaction history | ✅ |
| Order list with status filtering | ✅ |
| KMP shared business logic (Android + iOS) | ✅ |
| TDD unit & integration tests | ✅ |

---

## Quick Start

### Prerequisites

- Android Studio Meerkat or newer
- JDK 17+
- Xcode 15+ (for iOS build)

### Run Android

```bash
# Clone and open in Android Studio, then run 'androidApp'
git clone <your-repo-url>
cd kmm_pos
./gradlew :androidApp:installDebug
```

### Run Tests

```bash
./gradlew :shared:allTests
```

---

## Architecture

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for a full deep-dive covering:

- Clean Architecture layers
- SOLID principles mapping
- State management with StateFlow
- Offline-first strategy
- Sync design and conflict resolution
- Hardware simulation
- Multi-branch scalability

---

## Project Structure

```
kmm_pos/
├── shared/          ← KMP shared module (domain + data + presentation)
├── androidApp/      ← Jetpack Compose Android app
├── iosApp/          ← SwiftUI iOS app (framework consumer)
└── docs/            ← Architecture notes
```

---

## Tech Stack

| Library | Purpose |
|---|---|
| Kotlin Multiplatform | Shared business logic across Android & iOS |
| Jetpack Compose | Android declarative UI |
| SQLDelight 2.x | Type-safe SQL, offline storage |
| Koin 3.x | Dependency injection |
| Kotlin Coroutines + StateFlow | Async & reactive state |
| Turbine | Flow testing |
| Ktor (wired, not activated) | HTTP sync transport |
| kotlinx-datetime | Cross-platform timestamps |
| uuid (benasher44) | Cross-platform UUID generation |

---

## TDD Approach

Tests are written before implementation following the **Red → Green → Refactor** cycle.

Key test files:

- `CartUseCasesTest` — 12 tests covering add, remove, update quantity, calculate total, domain validation
- `PaymentUseCasesTest` — 5 tests covering payment amounts, change, status transitions, failure cases
- `ProductRepositoryImplTest` — 7 integration tests against in-memory SQLite

---

## AI Usage Transparency

This project was scaffolded with AI assistance (Claude). The following aspects were human-directed:

- Architecture decisions (Clean Architecture, outbox pattern, SOLID mapping)
- TDD test scenarios and assertions
- Offline-first strategy design
- Sync conflict resolution approach
- Multi-branch scalability considerations

The AI generated the boilerplate code following the specified architectural constraints. All design decisions are described in `ARCHITECTURE.md` and can be defended in a live technical session.
