# Login + Admin Side Implementation Plan

> Executed inline (native) at the user's request. **Spec:** `docs/superpowers/specs/2026-10-13-admin-side-design.md`

**Goal:** Login entry on home + admin area (dashboard, fertilizers, dealers, stock) over an in-memory mock-data store.

**Ruling:** The user said "go ahead, build it natively" right after approving the design, so this plan lists the tasks, interfaces and tests and the code is written during execution, TDD-first, instead of being pre-written here. Cost if wrong: less upfront review of the code.

## Global Constraints
- Data only from `assets/mock_data.json`; no SharedPreferences, SQLite, file writes or network.
- All UI text in Bangla in `strings.xml`; numbers via `BanglaNumber`.
- MVVM: Activities/Fragments → ViewModels → repositories → `MockDataStore`.
- Package root `com.sarbondhu.app`; minSdk 24.

## Tasks
1. **Store + models.** `Admin`, `ActivityEntry`; `Shop` gains `username`, `password`, `active`. `MockDataStore` (load once, mutable copies, next-id helpers). `FertilizerRepository` moves onto the store and hides inactive dealers. JSON gains admins + credentials. Tests: store loads once / retries after failure; inactive shop hidden; existing tests pass.
2. **Auth + Session.** `AuthRepository.login(Role, username, password) → LoginResult {SUCCESS, WRONG_CREDENTIALS, INACTIVE}`; `Session`. Tests: admin ok, dealer ok, wrong password, inactive dealer, role mismatch.
3. **Admin repositories.** `FertilizerRepository` add/update/delete (cascade); `DealerRepository` getAll/get/add/update/setActive/resetPassword (unique usernames); `StockRepository` getDealerStock/setStock/adjustStock/getLowStock/getTotalStock; `ActivityRepository` log/recent; `DataException`. Tests for every rule in the spec.
4. **Login UI.** Home app-bar icon, `LoginActivity` + `LoginViewModel`.
5. **Admin shell + dashboard.** `AdminActivity` (bottom nav, logout, session guard), `DashboardFragment` + VM.
6. **Fertilizer management.** List fragment, `FertilizerFormActivity` + VM (validation, delete confirm).
7. **Dealer management.** List fragment, `DealerFormActivity` + VM, activate/deactivate, reset password.
8. **Stock management.** Fragment with dealer picker, set/adjust dialogs, VM.
9. **Verify.** Full tests + build, emulator run-through, final review.
