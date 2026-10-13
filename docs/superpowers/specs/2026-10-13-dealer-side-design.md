# Sar Bondhu Android — Dealer Side (Phase 2)

**Date:** 2026-10-13
**Status:** Approved in conversation ("yes go ahead, build it natively")
**Builds on:** `2026-10-13-admin-side-design.md` (same in-memory store, session, MVVM).

## Goal
Dealers log in and run their shop: see their stock and sales figures, record
multi-fertilizer sales (stock deducted), browse sales history, and change their
password. Strictly mock data: `mock_data.json` only, in-memory, no
SharedPreferences / SQLite / files / network.

## Data
- `sales` array in `mock_data.json`. Each sale: `id`, `invoiceNo`, `shopId`,
  `customerName`, `customerPhone`, `daysAgo`, `time` ("HH:mm"), `items`
  (`fertilizerId`, `quantity`, `unitPrice`).
- Relative dates (`daysAgo` + `time`) are converted to real timestamps at load,
  so seeded "today" sales are always today.
- Each sale item stores a snapshot of fertilizer name, unit and unit price
  (seed items get name/unit filled at load), so history survives later price
  changes or fertilizer deletion.
- ~12 seeded sales across dealers: today, earlier this month, last month.

## Rules
1. A sale is validated in full before any change: cart not empty, every quantity
   > 0, dealer exists and is active, every fertilizer exists, and the dealer holds
   enough of each (duplicate lines are summed). Any failure → nothing changes.
2. On success: stock deducted, next invoice `SB-0001`-style number assigned,
   activity log entry written; farmers see the new stock immediately.
3. Dealers never add stock (no UI or API for it on the dealer side).
4. Change password requires the current password; new one ≥ 6 characters.
5. Dealer screens require a dealer session; data shown is only that dealer's.

## Screens (bottom navigation, shop name subtitle, logout)
- ড্যাশবোর্ড: today's sale count & revenue, this month's count & revenue,
  own stock with green/orange/red badges, 5 recent sales.
- নতুন বিক্রয়: in-stock fertilizers → quantity dialog → cart (remove, total),
  optional customer name/phone, "বিক্রয় সম্পন্ন করুন" → invoice confirmation.
- বিক্রয়ের ইতিহাস: all sales newest first, search (invoice / customer /
  fertilizer), total revenue, tap → invoice details.
- প্রোফাইল: shop details (read-only) + change password form.
- Login: dealer login opens this area (replaces "coming soon").

## Testing
JVM tests for sale creation (deduction, invoice, snapshots, all-or-nothing,
inactive dealer, empty/zero, duplicate lines), relative-date loading, dashboard
figures, search, change password, and seed-data consistency; emulator run-through.
