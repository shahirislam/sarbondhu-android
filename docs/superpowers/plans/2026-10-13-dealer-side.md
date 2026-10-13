# Dealer Side Implementation Plan

> Executed inline (native) at the user's request. **Spec:** `docs/superpowers/specs/2026-10-13-dealer-side-design.md`

**Ruling (same as phase 1):** the user asked to build straight after approving the design, so this plan lists tasks and interfaces; code is written test-first during execution.

## Global Constraints
- Data only from `assets/mock_data.json`; in-memory edits; no SharedPreferences, SQLite, files or network.
- Bangla-only UI text in `strings.xml`; numbers via `BanglaNumber`.
- MVVM; dealer screens guarded by a dealer `Session`.

## Tasks
1. **Sale models + store.** `Sale`, `SaleItem` (name/unit/price snapshot), `MockData.sales`; store resolves `daysAgo`/`time` to timestamps with an injectable clock, fills item snapshots, keeps sale-id and invoice counters. Seed ~12 sales. Tests: relative dates, snapshots, seed consistency.
2. **SaleRepository.** `createSale`, `getSales`, `getDealerDashboard`, `matches` (search). Tests for every rule.
3. **Change password.** `DealerRepository.changePassword(id, current, new)`. Tests.
4. **Login → dealer area.** `LoginViewModel` opens `DealerActivity` for dealers.
5. **Dealer shell + dashboard.**
6. **New sale screen** (cart, confirmation).
7. **Sales history + invoice details.**
8. **Profile + change password.**
9. **Verify:** tests, build, emulator run-through, final review.
