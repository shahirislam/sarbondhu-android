# Sar Bondhu Android — Login + Admin Side (Phase 1)

**Date:** 2026-10-13
**Status:** Approved in conversation ("yes go ahead, build it natively")
**Phase 2 (later, separate spec):** dealer side.

## Goal

Add a login entry point on the home screen and a full admin area. Data stays
strictly mock: everything comes from `assets/mock_data.json`, edits live in
memory for the running session only. No SharedPreferences, no SQLite, no files
written, no network.

## Decisions

| Topic | Decision |
|---|---|
| Persistence | In-memory only. Every launch starts fresh from `mock_data.json`. |
| Admin scope | Dashboard, manage fertilizers, manage dealers, manage stock. |
| Login entry | Person icon at the right of the home app bar (slogan stays centred). |
| Accounts | New `admins` array; existing `shops` gain `username`, `password`, `active`. Shops = dealers. |
| Passwords | Plain text in mock JSON (mock data; documented, not secure). Seeds: `admin`/`admin123`, `dealer1`…`dealer6`/`dealer123`. |
| Session | In-memory singleton; logout clears; app restart = logged out. |
| Dealer login | Recognised, but shows "ডিলার অংশ শীঘ্রই আসছে" until phase 2. |

## Data model additions (`mock_data.json`)

```json
"admins": [{ "id": 1, "username": "admin", "password": "admin123", "name": "প্রধান অ্যাডমিন" }],
"shops":  [{ "id": 1, ..., "username": "dealer1", "password": "dealer123", "active": true }]
```
Missing `active` means active.

## Architecture

- `MockDataStore` (singleton): loads JSON once via `DataLoader`, copies into
  mutable in-memory lists (fertilizers, shops, stocks, admins, activity log).
  All repository access synchronises on the store.
- Repositories over the store: `FertilizerRepository` (existing reads + add /
  update / delete), `DealerRepository`, `StockRepository`, `AuthRepository`,
  `ActivityRepository`.
- Models stay immutable; an edit replaces the object in the list.
- Business-rule violations throw `DataException` with a `Reason` enum; the UI
  maps reasons to Bangla strings. Form field validation lives in ViewModels.
- `Session` singleton: role (ADMIN / DEALER), user id, display name.

## Business rules

1. Stock can never go negative; an adjust that would makes it fail.
2. Deleting a fertilizer removes all its stock rows.
3. Dealer usernames are unique (case-insensitive) and cannot equal an admin username.
4. Inactive dealers cannot log in and are hidden from the farmer screens
   (shop lists and shop counts).
5. Low stock = quantity below 100 (0 included) for active dealers.
6. Every admin change writes a Bangla activity-log entry (newest first).
7. Admin screens require an admin session; otherwise they close.

## Screens

- **Home:** login icon in the app bar → Login.
- **Login:** ডিলার / অ্যাডমিন toggle, username, password, Bangla errors
  (wrong credentials, inactive dealer, empty fields).
- **Admin** (bottom navigation, toolbar with logout):
  - ড্যাশবোর্ড: 4 stat cards (fertilizers, active dealers, total stock kg,
    low-stock count), low-stock list, recent activity.
  - সার: list → add/edit form (name, type, NPK, price, unit, description,
    usage, crops comma-separated); delete with confirmation.
  - ডিলার: list with active badge → add/edit form (shop, owner, phone,
    district, upazila, address, username, password on add), activate /
    deactivate, reset password.
  - মজুদ: choose dealer → every fertilizer with quantity & price; "সেট"
    (absolute quantity + price) and "যোগ/বিয়োগ" (delta) dialogs.

## Testing

JVM unit tests for the store, auth, fertilizer/dealer/stock rules, activity
log, and validation helpers; manual run-through on the Seagate emulator.
