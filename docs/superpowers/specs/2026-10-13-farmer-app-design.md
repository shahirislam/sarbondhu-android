# Sar Bondhu Android — Farmer Version Design

**Date:** 2026-10-13
**Status:** Approved in conversation, pending spec review

## Goal

An Android app (Java, MVVM) for Sar Bondhu, entirely in Bangla. On launch it
shows the farmer version: a list of fertilizers. Tapping a fertilizer shows its
details and which shops stock it and how much. All data comes from one static
JSON mock file.

## Scope

In scope: fertilizer list screen, fertilizer detail screen (with shop stock list).

Out of scope: search, filtering, crop recommendation, login, dealer/admin
versions, networking, local database.

## Data — `app/src/main/assets/mock_data.json`

Single source of truth for all app data. All text values in Bangla.

```json
{
  "fertilizers": [
    { "id": 1, "name": "ইউরিয়া", "category": "নাইট্রোজেন জাতীয়",
      "npkRatio": "৪৬-০-০", "description": "...", "usage": "...",
      "recommendedCrops": ["ধান", "গম"], "unitPrice": 27.0, "unit": "কেজি" }
  ],
  "shops": [
    { "id": 1, "shopName": "...", "ownerName": "...", "phone": "01711000000",
      "district": "...", "upazila": "...", "address": "..." }
  ],
  "stocks": [
    { "shopId": 1, "fertilizerId": 1, "quantity": 500.0, "sellingPrice": 27.0 }
  ]
}
```

Content: ~8 fertilizers, ~6 shops, stock rows linking them (including some
zero-quantity rows), adapted from the Python app's seed data.

## Architecture (MVVM)

Package `com.sarbondhu.app`:

| Unit | Responsibility |
|---|---|
| `data/model/Fertilizer`, `Shop`, `Stock` | Gson POJOs mirroring the JSON |
| `data/model/MockData` | Root JSON object (three lists) |
| `data/model/ShopStock` | Join of `Shop` + quantity + selling price for one fertilizer |
| `data/source/JsonDataSource` | Reads and parses `mock_data.json` from assets (`InputStream` → `MockData`) |
| `data/repository/FertilizerRepository` | Singleton; loads data once on a background executor, caches it, exposes `getFertilizers()`, `getFertilizer(id)`, `getShopStocks(fertilizerId)`, `getShopCount(fertilizerId)` via callbacks/LiveData |
| `ui/fertilizerlist/FertilizerListActivity` | Launcher; RecyclerView of fertilizer cards |
| `ui/fertilizerlist/FertilizerListViewModel` | Exposes `LiveData<UiState<List<FertilizerItem>>>` (loading / success / error) |
| `ui/fertilizerlist/FertilizerAdapter` | ListAdapter; card shows name, category, price per unit, number of shops in stock |
| `ui/fertilizerdetail/FertilizerDetailActivity` | Receives fertilizer id via Intent extra; shows details + shop list |
| `ui/fertilizerdetail/FertilizerDetailViewModel` | Exposes fertilizer `LiveData` and `LiveData<List<ShopStock>>` |
| `ui/fertilizerdetail/ShopStockAdapter` | Shop card: name, owner, upazila/district, address, quantity, price, call button |
| `util/BanglaNumber` | Converts digits to Bangla (০-৯) and formats quantities/prices |

Data flow: Activity → ViewModel → Repository → JsonDataSource → assets.
Activities only observe LiveData; no data logic in UI classes.

## UI

- XML layouts, Material Components 3, green agriculture theme, light mode.
- All strings in Bangla in `res/values/strings.xml`; all numbers shown in
  Bangla digits.
- Detail screen: header (name, category, NPK, price), sections for বিবরণ,
  ব্যবহারবিধি, উপযোগী ফসল (chips), then দোকানসমূহ list. Shops sorted by
  quantity descending; zero-stock shops show "স্টক নেই" badge.
- Tap phone → `ACTION_DIAL` intent (no permission needed).

## Error handling

- JSON missing/malformed → list screen shows error message in Bangla.
- Unknown fertilizer id on detail → error message and finish-safe state.
- Fertilizer with no stock rows → "কোনো দোকানে পাওয়া যায়নি" empty state.

## Build

Gradle wrapper 9.7.1, AGP 9.4.1 (local JDK is 25, which needs Gradle 9.1+),
Java 17 source/target, minSdk 24, targetSdk/compileSdk 35.
Dependencies: AppCompat, Material, RecyclerView, Lifecycle (ViewModel/LiveData),
Gson, JUnit.

## Testing

JVM unit tests (JUnit 4):
- `JsonDataSource` parses a test JSON stream correctly.
- Repository join logic: `getShopStocks` returns correct shops, quantities,
  sort order; unknown id → empty list.
- `BanglaNumber` digit conversion and formatting.

Verification: `./gradlew testDebugUnitTest assembleDebug` succeeds.
