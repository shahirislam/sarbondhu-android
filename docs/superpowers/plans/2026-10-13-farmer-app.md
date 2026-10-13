# Sar Bondhu Farmer App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Bangla-language Android app (Java, MVVM) that lists fertilizers and, on tap, shows fertilizer details plus which shops stock it and how much — all from one static JSON mock file.

**Architecture:** `assets/mock_data.json` → `JsonDataSource` (Gson) → `FertilizerRepository` (singleton, in-memory cache, synchronous query methods) → `AndroidViewModel`s (run repository calls on a background executor, publish `LiveData<UiState<T>>`) → Activities (observe and render only).

**Tech Stack:** Java 17, AGP 9.4.1, Gradle 9.7.1, AppCompat 1.7.1, Material 1.13.0, RecyclerView 1.4.0, Lifecycle 2.9.4, Gson 2.13.2, JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-10-13-farmer-app-design.md`

All paths below are relative to `/Volumes/Seagate/uni_stuff/sarbondhu_mobileApp/`.
The folder is not a git repository and the user has not asked for commits, so tasks end with a verification step instead of a commit.

## Global Constraints

- Every user-visible string is Bangla and lives in `app/src/main/res/values/strings.xml`; no hard-coded UI text in Java or layouts.
- Every number shown to the user goes through `BanglaNumber` (Bangla digits ০-৯).
- All app data comes from `app/src/main/assets/mock_data.json`; no data in Java code.
- MVVM: Activities never call the repository; ViewModels never touch Views.
- Package root `com.sarbondhu.app`; minSdk 24, targetSdk/compileSdk 35, Java 17 source/target.
- Scope: only the fertilizer list and detail screens (no search, filter, login, recommendations).

## Review Focus

1. Malformed or empty JSON file → list screen shows Bangla error + retry, app does not crash. (Task 3 tests `parse` throws `IOException`; Task 4 tests failure is not cached so retry works.)
2. Stock row referencing a shop id that does not exist → row silently skipped, not a crash. (Task 4 test.)
3. Fertilizer with zero stock rows, or only zero-quantity rows → list card says "কোনো দোকানে স্টক নেই", detail shows empty-state text. (Task 4 tests count = 0 and empty list.)
4. Missing optional JSON fields (no `recommendedCrops`, no `phone`) → empty list / empty string, no NPE. (Task 3 test; UI hides call button for empty phone.)
5. Screen rotation on detail screen → no duplicate load. (Detail ViewModel `started` flag; checked manually in Task 7.)

---

### Task 1: Project scaffold that builds

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`, `local.properties`, `.gitignore`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/colors.xml`, `themes.xml`, `strings.xml`, `app/src/main/res/values-v35/themes.xml`, `app/src/main/res/drawable/bg_badge_out.xml`
- Create: `app/src/main/java/com/sarbondhu/app/ui/fertilizerlist/FertilizerListActivity.java` (stub)
- Create: Gradle wrapper (`gradlew`, `gradle/wrapper/*`)

**Interfaces:**
- Produces: buildable module `:app`, theme `Theme.SarBondhu`, style `SectionLabel`, all string resources used by later tasks, `@drawable/bg_badge_out`.

- [ ] **Step 1: Write Gradle files**

`settings.gradle`:
```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SarBondhu"
include ':app'
```

`build.gradle`:
```groovy
plugins {
    id 'com.android.application' version '9.4.1' apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
```

`local.properties`:
```properties
sdk.dir=/Users/shahir/Library/Android/sdk
```

`.gitignore`:
```
.gradle/
build/
local.properties
.idea/
*.iml
.DS_Store
```

`app/build.gradle`:
```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace = 'com.sarbondhu.app'
    compileSdk = 35

    defaultConfig {
        applicationId = 'com.sarbondhu.app'
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = '1.0'
    }

    buildTypes {
        release {
            minifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.13.0'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.9.4'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.9.4'
    implementation 'com.google.code.gson:gson:2.13.2'

    testImplementation 'junit:junit:4.13.2'
}
```

If AGP rejects compileSdk 35 for a dependency (error names a minimum compileSdk), raise `compileSdk` to that value (platform 36 is installed) and keep `targetSdk = 35`.

- [ ] **Step 2: Generate the Gradle wrapper**

```bash
cd "$SCRATCH" && curl -sSLO https://services.gradle.org/distributions/gradle-9.7.1-bin.zip && unzip -q gradle-9.7.1-bin.zip
cd /Volumes/Seagate/uni_stuff/sarbondhu_mobileApp && "$SCRATCH/gradle-9.7.1/bin/gradle" wrapper --gradle-version 9.7.1
```
(`$SCRATCH` = the session scratchpad directory.) Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` exist.

- [ ] **Step 3: Write resources**

`app/src/main/res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="green_900">#1B5E20</color>
    <color name="green_800">#2E7D32</color>
    <color name="green_700">#388E3C</color>
    <color name="green_100">#DCEFD9</color>
    <color name="amber_700">#F9A825</color>
    <color name="red_600">#E53935</color>
    <color name="background">#F5F8F2</color>
    <color name="text_secondary">#5F6B5C</color>
    <color name="white">#FFFFFF</color>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SarBondhu" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/green_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorPrimaryContainer">@color/green_100</item>
        <item name="colorOnPrimaryContainer">@color/green_900</item>
        <item name="colorSecondary">@color/amber_700</item>
        <item name="android:colorBackground">@color/background</item>
        <item name="colorSurface">@color/white</item>
        <item name="android:statusBarColor">@color/green_900</item>
    </style>

    <style name="SectionLabel">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:layout_marginTop">20dp</item>
        <item name="android:layout_marginBottom">6dp</item>
        <item name="android:textAppearance">?attr/textAppearanceTitleMedium</item>
        <item name="android:textColor">?attr/colorPrimary</item>
    </style>
</resources>
```

`app/src/main/res/values-v35/themes.xml` (targetSdk 35 forces edge-to-edge on Android 15; opt out so the toolbar is not drawn under the status bar):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SarBondhu" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/green_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorPrimaryContainer">@color/green_100</item>
        <item name="colorOnPrimaryContainer">@color/green_900</item>
        <item name="colorSecondary">@color/amber_700</item>
        <item name="android:colorBackground">@color/background</item>
        <item name="colorSurface">@color/white</item>
        <item name="android:statusBarColor">@color/green_900</item>
        <item name="android:windowOptOutEdgeToEdgeEnforcement">true</item>
    </style>
</resources>
```

`app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">সার বন্ধু</string>

    <string name="list_title">সারের তালিকা</string>
    <string name="list_subtitle">কৃষকের জন্য সারের তথ্য</string>
    <string name="empty_fertilizers">কোনো সার পাওয়া যায়নি</string>
    <string name="error_load_data">তথ্য লোড করা যায়নি।</string>
    <string name="error_not_found">এই সারের তথ্য পাওয়া যায়নি।</string>
    <string name="retry">আবার চেষ্টা করুন</string>

    <string name="price_per_unit">৳%1$s / %2$s</string>
    <string name="shop_count">%1$sটি দোকানে পাওয়া যাচ্ছে</string>
    <string name="not_available_anywhere">কোনো দোকানে স্টক নেই</string>

    <string name="nutrient">পুষ্টি উপাদান: %1$s</string>
    <string name="fixed_price">নির্ধারিত মূল্য: ৳%1$s / %2$s</string>
    <string name="section_description">বিবরণ</string>
    <string name="section_usage">ব্যবহারবিধি</string>
    <string name="section_crops">উপযোগী ফসল</string>
    <string name="section_shops">কোথায় পাওয়া যাবে</string>
    <string name="no_shops">কোনো দোকানে এই সার পাওয়া যায়নি</string>

    <string name="owner">মালিক: %1$s</string>
    <string name="location">%1$s, %2$s</string>
    <string name="stock_quantity">মজুদ: %1$s %2$s</string>
    <string name="stock_out">স্টক নেই</string>
    <string name="selling_price">বিক্রয় মূল্য: ৳%1$s / %2$s</string>
    <string name="call">কল করুন: %1$s</string>
</resources>
```

`app/src/main/res/drawable/bg_badge_out.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/red_600" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 4: Manifest and stub launcher activity**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SarBondhu">

        <activity
            android:name=".ui.fertilizerlist.FertilizerListActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.fertilizerdetail.FertilizerDetailActivity"
            android:exported="false"
            android:parentActivityName=".ui.fertilizerlist.FertilizerListActivity" />
    </application>
</manifest>
```

Stub `FertilizerListActivity.java` (replaced in Task 5):
```java
package com.sarbondhu.app.ui.fertilizerlist;

import androidx.appcompat.app.AppCompatActivity;

public class FertilizerListActivity extends AppCompatActivity {
}
```

Stub `app/src/main/java/com/sarbondhu/app/ui/fertilizerdetail/FertilizerDetailActivity.java` (replaced in Task 6):
```java
package com.sarbondhu.app.ui.fertilizerdetail;

import androidx.appcompat.app.AppCompatActivity;

public class FertilizerDetailActivity extends AppCompatActivity {
}
```

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.

---

### Task 2: BanglaNumber utility

**Files:**
- Create: `app/src/main/java/com/sarbondhu/app/util/BanglaNumber.java`
- Test: `app/src/test/java/com/sarbondhu/app/util/BanglaNumberTest.java`

**Interfaces:**
- Produces: `static String BanglaNumber.toBangla(String)`, `static String BanglaNumber.format(double)`, `static String BanglaNumber.format(int)`.

- [ ] **Step 1: Write the failing test**

```java
package com.sarbondhu.app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BanglaNumberTest {

    @Test
    public void toBangla_convertsAllAsciiDigits() {
        assertEquals("০১৭১১২৩৪৫৬৭", BanglaNumber.toBangla("01711234567"));
    }

    @Test
    public void toBangla_keepsNonDigitCharacters() {
        assertEquals("১৮-৪৬-০ কেজি", BanglaNumber.toBangla("18-46-0 কেজি"));
    }

    @Test
    public void toBangla_nullBecomesEmpty() {
        assertEquals("", BanglaNumber.toBangla(null));
    }

    @Test
    public void format_wholeDoubleHasNoDecimals() {
        assertEquals("৫০০", BanglaNumber.format(500.0));
    }

    @Test
    public void format_keepsMeaningfulDecimals() {
        assertEquals("২৭.৫", BanglaNumber.format(27.5));
    }

    @Test
    public void format_roundsToTwoDecimals() {
        assertEquals("১২৩৪.৫৭", BanglaNumber.format(1234.567));
    }

    @Test
    public void format_zero() {
        assertEquals("০", BanglaNumber.format(0.0));
    }

    @Test
    public void format_int() {
        assertEquals("৩", BanglaNumber.format(3));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.util.BanglaNumberTest'`
Expected: compilation FAIL, `cannot find symbol BanglaNumber`.

- [ ] **Step 3: Implement**

```java
package com.sarbondhu.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts numbers to Bangla digits for display. */
public final class BanglaNumber {

    private static final char[] DIGITS = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};

    private BanglaNumber() {
    }

    public static String toBangla(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            out.append(c >= '0' && c <= '9' ? DIGITS[c - '0'] : c);
        }
        return out.toString();
    }

    /** Formats with at most two decimals and no trailing zeros, e.g. 500.0 → ৫০০, 27.5 → ২৭.৫. */
    public static String format(double value) {
        String plain = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        return toBangla(plain);
    }

    public static String format(int value) {
        return toBangla(String.valueOf(value));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.util.BanglaNumberTest'`
Expected: PASS (8 tests).

---

### Task 3: Models, JSON data source, mock data file

**Files:**
- Create: `app/src/main/java/com/sarbondhu/app/data/model/Fertilizer.java`, `Shop.java`, `Stock.java`, `MockData.java`
- Create: `app/src/main/java/com/sarbondhu/app/data/source/DataLoader.java`, `JsonDataSource.java`
- Create: `app/src/main/assets/mock_data.json`
- Test: `app/src/test/java/com/sarbondhu/app/data/source/JsonDataSourceTest.java`

**Interfaces:**
- Produces:
  - `Fertilizer(int id, String name, String category, String npkRatio, String description, String usage, List<String> recommendedCrops, double unitPrice, String unit)` with getters `getId() getName() getCategory() getNpkRatio() getDescription() getUsage() getRecommendedCrops() getUnitPrice() getUnit()`; string getters never return null, list getter never null.
  - `Shop(int id, String shopName, String ownerName, String phone, String district, String upazila, String address)` with getters `getId() getShopName() getOwnerName() getPhone() getDistrict() getUpazila() getAddress()`; never null.
  - `Stock(int shopId, int fertilizerId, double quantity, double sellingPrice)` with getters.
  - `MockData(List<Fertilizer>, List<Shop>, List<Stock>)` with `getFertilizers() getShops() getStocks()`; never null.
  - `interface DataLoader { MockData load() throws IOException; }`
  - `JsonDataSource(AssetManager)` implements `DataLoader`; `static MockData parse(InputStream) throws IOException`.

- [ ] **Step 1: Write the failing test**

```java
package com.sarbondhu.app.data.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class JsonDataSourceTest {

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void parse_readsAllSections() throws IOException {
        String json = "{"
                + "\"fertilizers\":[{\"id\":1,\"name\":\"ইউরিয়া\",\"category\":\"নাইট্রোজেন\","
                + "\"npkRatio\":\"৪৬-০-০\",\"description\":\"d\",\"usage\":\"u\","
                + "\"recommendedCrops\":[\"ধান\",\"গম\"],\"unitPrice\":27.0,\"unit\":\"কেজি\"}],"
                + "\"shops\":[{\"id\":5,\"shopName\":\"রহমান\",\"ownerName\":\"o\",\"phone\":\"017\","
                + "\"district\":\"বগুড়া\",\"upazila\":\"শেরপুর\",\"address\":\"a\"}],"
                + "\"stocks\":[{\"shopId\":5,\"fertilizerId\":1,\"quantity\":500.0,\"sellingPrice\":28.5}]"
                + "}";

        MockData data = JsonDataSource.parse(stream(json));

        Fertilizer f = data.getFertilizers().get(0);
        assertEquals(1, f.getId());
        assertEquals("ইউরিয়া", f.getName());
        assertEquals(2, f.getRecommendedCrops().size());
        assertEquals(27.0, f.getUnitPrice(), 0.0001);
        assertEquals("বগুড়া", data.getShops().get(0).getDistrict());
        Stock s = data.getStocks().get(0);
        assertEquals(5, s.getShopId());
        assertEquals(500.0, s.getQuantity(), 0.0001);
        assertEquals(28.5, s.getSellingPrice(), 0.0001);
    }

    @Test
    public void parse_missingOptionalFieldsAreEmptyNotNull() throws IOException {
        String json = "{\"fertilizers\":[{\"id\":1,\"name\":\"x\"}],\"shops\":[{\"id\":1}]}";

        MockData data = JsonDataSource.parse(stream(json));

        Fertilizer f = data.getFertilizers().get(0);
        assertTrue(f.getRecommendedCrops().isEmpty());
        assertEquals("", f.getUsage());
        assertEquals("", data.getShops().get(0).getPhone());
        assertTrue(data.getStocks().isEmpty());
    }

    @Test(expected = IOException.class)
    public void parse_malformedJsonThrowsIOException() throws IOException {
        JsonDataSource.parse(stream("{\"fertilizers\": [ {"));
    }

    @Test(expected = IOException.class)
    public void parse_emptyFileThrowsIOException() throws IOException {
        JsonDataSource.parse(stream(""));
    }

    @Test
    public void realAssetFile_isValidAndConsistent() throws IOException {
        MockData data;
        try (InputStream in = new FileInputStream("src/main/assets/mock_data.json")) {
            data = JsonDataSource.parse(in);
        }

        assertEquals(8, data.getFertilizers().size());
        assertEquals(6, data.getShops().size());
        assertFalse(data.getStocks().isEmpty());

        Set<Integer> fertilizerIds = new HashSet<>();
        for (Fertilizer f : data.getFertilizers()) {
            assertTrue("duplicate fertilizer id " + f.getId(), fertilizerIds.add(f.getId()));
            assertFalse(f.getName().isEmpty());
        }
        Set<Integer> shopIds = new HashSet<>();
        for (Shop s : data.getShops()) {
            assertTrue("duplicate shop id " + s.getId(), shopIds.add(s.getId()));
            assertNotNull(s.getPhone());
        }
        for (Stock s : data.getStocks()) {
            assertTrue("unknown shop " + s.getShopId(), shopIds.contains(s.getShopId()));
            assertTrue("unknown fertilizer " + s.getFertilizerId(),
                    fertilizerIds.contains(s.getFertilizerId()));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.data.source.JsonDataSourceTest'`
Expected: compilation FAIL, `cannot find symbol JsonDataSource`.

- [ ] **Step 3: Implement models**

`Fertilizer.java`:
```java
package com.sarbondhu.app.data.model;

import java.util.Collections;
import java.util.List;

public class Fertilizer {

    private int id;
    private String name;
    private String category;
    private String npkRatio;
    private String description;
    private String usage;
    private List<String> recommendedCrops;
    private double unitPrice;
    private String unit;

    public Fertilizer() {
    }

    public Fertilizer(int id, String name, String category, String npkRatio, String description,
                      String usage, List<String> recommendedCrops, double unitPrice, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.npkRatio = npkRatio;
        this.description = description;
        this.usage = usage;
        this.recommendedCrops = recommendedCrops;
        this.unitPrice = unitPrice;
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return orEmpty(name);
    }

    public String getCategory() {
        return orEmpty(category);
    }

    public String getNpkRatio() {
        return orEmpty(npkRatio);
    }

    public String getDescription() {
        return orEmpty(description);
    }

    public String getUsage() {
        return orEmpty(usage);
    }

    public List<String> getRecommendedCrops() {
        return recommendedCrops == null ? Collections.emptyList() : recommendedCrops;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getUnit() {
        return orEmpty(unit);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
```

`Shop.java`:
```java
package com.sarbondhu.app.data.model;

public class Shop {

    private int id;
    private String shopName;
    private String ownerName;
    private String phone;
    private String district;
    private String upazila;
    private String address;

    public Shop() {
    }

    public Shop(int id, String shopName, String ownerName, String phone,
                String district, String upazila, String address) {
        this.id = id;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.district = district;
        this.upazila = upazila;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public String getShopName() {
        return orEmpty(shopName);
    }

    public String getOwnerName() {
        return orEmpty(ownerName);
    }

    public String getPhone() {
        return orEmpty(phone);
    }

    public String getDistrict() {
        return orEmpty(district);
    }

    public String getUpazila() {
        return orEmpty(upazila);
    }

    public String getAddress() {
        return orEmpty(address);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
```

`Stock.java`:
```java
package com.sarbondhu.app.data.model;

/** How much of one fertilizer one shop has. */
public class Stock {

    private int shopId;
    private int fertilizerId;
    private double quantity;
    private double sellingPrice;

    public Stock() {
    }

    public Stock(int shopId, int fertilizerId, double quantity, double sellingPrice) {
        this.shopId = shopId;
        this.fertilizerId = fertilizerId;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
    }

    public int getShopId() {
        return shopId;
    }

    public int getFertilizerId() {
        return fertilizerId;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }
}
```

`MockData.java`:
```java
package com.sarbondhu.app.data.model;

import java.util.Collections;
import java.util.List;

/** Root object of mock_data.json. */
public class MockData {

    private List<Fertilizer> fertilizers;
    private List<Shop> shops;
    private List<Stock> stocks;

    public MockData() {
    }

    public MockData(List<Fertilizer> fertilizers, List<Shop> shops, List<Stock> stocks) {
        this.fertilizers = fertilizers;
        this.shops = shops;
        this.stocks = stocks;
    }

    public List<Fertilizer> getFertilizers() {
        return fertilizers == null ? Collections.emptyList() : fertilizers;
    }

    public List<Shop> getShops() {
        return shops == null ? Collections.emptyList() : shops;
    }

    public List<Stock> getStocks() {
        return stocks == null ? Collections.emptyList() : stocks;
    }
}
```

- [ ] **Step 4: Implement data source**

`DataLoader.java`:
```java
package com.sarbondhu.app.data.source;

import com.sarbondhu.app.data.model.MockData;

import java.io.IOException;

/** Supplies the app's data. Swap the implementation to move from mock JSON to a real API. */
public interface DataLoader {
    MockData load() throws IOException;
}
```

`JsonDataSource.java`:
```java
package com.sarbondhu.app.data.source;

import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sarbondhu.app.data.model.MockData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Reads mock_data.json from the app's assets folder. */
public class JsonDataSource implements DataLoader {

    public static final String FILE_NAME = "mock_data.json";

    private final AssetManager assets;

    public JsonDataSource(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public MockData load() throws IOException {
        try (InputStream in = assets.open(FILE_NAME)) {
            return parse(in);
        }
    }

    public static MockData parse(InputStream in) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            MockData data = new Gson().fromJson(reader, MockData.class);
            if (data == null) {
                throw new IOException("Data file is empty");
            }
            return data;
        } catch (JsonParseException e) {
            throw new IOException("Data file is malformed", e);
        }
    }
}
```

- [ ] **Step 5: Write the mock data file**

`app/src/main/assets/mock_data.json` — fertilizer 8 deliberately has no stock rows (empty-state demo); several rows have quantity 0 (out-of-stock badge demo).
```json
{
  "fertilizers": [
    {
      "id": 1,
      "name": "ইউরিয়া",
      "category": "নাইট্রোজেন জাতীয় সার",
      "npkRatio": "৪৬-০-০",
      "description": "উচ্চ নাইট্রোজেনযুক্ত সার। বাংলাদেশে ধান চাষে সবচেয়ে বেশি ব্যবহৃত হয়। গাছের পাতা সবুজ ও সতেজ রাখে।",
      "usage": "প্রতি হেক্টরে ২৫০-৩০০ কেজি কয়েক কিস্তিতে প্রয়োগ করুন। কুশি গজানো ও থোড় আসার সময় প্রয়োগ সবচেয়ে কার্যকর।",
      "recommendedCrops": ["ধান", "গম", "ভুট্টা", "পাট", "আখ"],
      "unitPrice": 27.0,
      "unit": "কেজি"
    },
    {
      "id": 2,
      "name": "টিএসপি (ট্রিপল সুপার ফসফেট)",
      "category": "ফসফরাস জাতীয় সার",
      "npkRatio": "০-৪৬-০",
      "description": "ফসফরাসের ঘন উৎস। শিকড়ের বৃদ্ধি ঘটায় এবং ফলন বাড়ায়।",
      "usage": "প্রতি হেক্টরে ১৫০-২০০ কেজি চারা রোপণের আগে জমি তৈরির সময় মূল সার হিসেবে প্রয়োগ করুন।",
      "recommendedCrops": ["ধান", "গম", "আলু", "সরিষা", "সবজি"],
      "unitPrice": 27.0,
      "unit": "কেজি"
    },
    {
      "id": 3,
      "name": "এমওপি (মিউরিয়েট অব পটাশ)",
      "category": "পটাশ জাতীয় সার",
      "npkRatio": "০-০-৬০",
      "description": "পটাশিয়ামের প্রধান উৎস। ফসলের মান উন্নত করে ও রোগ প্রতিরোধ ক্ষমতা বাড়ায়।",
      "usage": "প্রতি হেক্টরে ৮০-১০০ কেজি প্রয়োগ করুন। বেলে মাটিতে দুই কিস্তিতে প্রয়োগ করা ভালো।",
      "recommendedCrops": ["ধান", "আলু", "কলা", "আখ", "তামাক"],
      "unitPrice": 20.0,
      "unit": "কেজি"
    },
    {
      "id": 4,
      "name": "ডিএপি (ডাই-অ্যামোনিয়াম ফসফেট)",
      "category": "মিশ্র সার",
      "npkRatio": "১৮-৪৬-০",
      "description": "নাইট্রোজেন ও ফসফরাস দুটোই সরবরাহ করে এমন ঘন মিশ্র সার।",
      "usage": "প্রতি হেক্টরে ১০০-১৫০ কেজি বীজ বপনের আগে মূল সার হিসেবে প্রয়োগ করুন।",
      "recommendedCrops": ["গম", "ভুট্টা", "সরিষা", "সবজি", "তুলা"],
      "unitPrice": 21.0,
      "unit": "কেজি"
    },
    {
      "id": 5,
      "name": "জিংক সালফেট",
      "category": "অণুপুষ্টি সার",
      "npkRatio": "জিংক ২১%",
      "description": "মাটির দস্তার অভাব পূরণ করে। ধান চাষে বিশেষভাবে প্রয়োজনীয়।",
      "usage": "প্রতি হেক্টরে ২০-২৫ কেজি প্রয়োগ করুন। ০.৫% দ্রবণ পাতায় স্প্রে করেও ব্যবহার করা যায়।",
      "recommendedCrops": ["ধান", "গম", "ভুট্টা", "আলু"],
      "unitPrice": 150.0,
      "unit": "কেজি"
    },
    {
      "id": 6,
      "name": "জিপসাম (ক্যালসিয়াম সালফেট)",
      "category": "অন্যান্য",
      "npkRatio": "ক্যালসিয়াম ২২%, সালফার ১৮%",
      "description": "ক্যালসিয়াম ও সালফার সরবরাহ করে। মাটির গঠন উন্নত করে ও চিনাবাদামের ফলন বাড়ায়।",
      "usage": "প্রতি হেক্টরে ২০০-২৫০ কেজি জমি তৈরির সময় প্রয়োগ করুন।",
      "recommendedCrops": ["চিনাবাদাম", "সরিষা", "পেঁয়াজ", "রসুন", "সবজি"],
      "unitPrice": 15.0,
      "unit": "কেজি"
    },
    {
      "id": 7,
      "name": "বোরিক এসিড",
      "category": "অণুপুষ্টি সার",
      "npkRatio": "বোরন ১৭%",
      "description": "বোরনের অভাব পূরণ করে। ফুল ও ফল ঝরে পড়া রোধ করে।",
      "usage": "প্রতি হেক্টরে ১০-১২ কেজি জমি তৈরির সময় প্রয়োগ করুন। অতিরিক্ত প্রয়োগ ক্ষতিকর।",
      "recommendedCrops": ["সরিষা", "গম", "ফুলকপি", "বাঁধাকপি"],
      "unitPrice": 250.0,
      "unit": "কেজি"
    },
    {
      "id": 8,
      "name": "জৈব সার (ভার্মিকম্পোস্ট)",
      "category": "জৈব সার",
      "npkRatio": "প্রাকৃতিক জৈব উপাদান",
      "description": "কেঁচো দিয়ে তৈরি জৈব সার। মাটির উর্বরতা ও পানি ধারণ ক্ষমতা বাড়ায়।",
      "usage": "প্রতি হেক্টরে ২-৩ টন জমি তৈরির সময় মাটির সাথে মিশিয়ে দিন।",
      "recommendedCrops": ["সবজি", "ফল", "ধান", "ফুল"],
      "unitPrice": 15.0,
      "unit": "কেজি"
    }
  ],
  "shops": [
    {
      "id": 1,
      "shopName": "মেসার্স রহমান ট্রেডার্স",
      "ownerName": "মো. আব্দুর রহমান",
      "phone": "01711234567",
      "district": "বগুড়া",
      "upazila": "শেরপুর",
      "address": "বাসস্ট্যান্ড মোড়, শেরপুর"
    },
    {
      "id": 2,
      "shopName": "কৃষক বন্ধু সার ঘর",
      "ownerName": "মো. কামাল হোসেন",
      "phone": "01812345678",
      "district": "রাজশাহী",
      "upazila": "পবা",
      "address": "নওহাটা বাজার, পবা"
    },
    {
      "id": 3,
      "shopName": "মেসার্স আল-আমিন এন্টারপ্রাইজ",
      "ownerName": "আল-আমিন মিয়া",
      "phone": "01913456789",
      "district": "ময়মনসিংহ",
      "upazila": "ত্রিশাল",
      "address": "কলেজ রোড, ত্রিশাল"
    },
    {
      "id": 4,
      "shopName": "সবুজ বাংলা কৃষি বিতান",
      "ownerName": "শাহানা বেগম",
      "phone": "01614567890",
      "district": "রংপুর",
      "upazila": "মিঠাপুকুর",
      "address": "শঠিবাড়ী বাজার, মিঠাপুকুর"
    },
    {
      "id": 5,
      "shopName": "মেসার্স মিতালী ট্রেডার্স",
      "ownerName": "মো. শফিকুল ইসলাম",
      "phone": "01515678901",
      "district": "যশোর",
      "upazila": "ঝিকরগাছা",
      "address": "বাজার রোড, ঝিকরগাছা"
    },
    {
      "id": 6,
      "shopName": "ভাই ভাই সার ও বীজ ভান্ডার",
      "ownerName": "মো. জসিম উদ্দিন",
      "phone": "01716789012",
      "district": "কুমিল্লা",
      "upazila": "চান্দিনা",
      "address": "পুরাতন বাজার, চান্দিনা"
    }
  ],
  "stocks": [
    { "shopId": 1, "fertilizerId": 1, "quantity": 500.0, "sellingPrice": 27.0 },
    { "shopId": 1, "fertilizerId": 2, "quantity": 200.0, "sellingPrice": 27.0 },
    { "shopId": 1, "fertilizerId": 3, "quantity": 150.0, "sellingPrice": 20.0 },
    { "shopId": 1, "fertilizerId": 4, "quantity": 0.0, "sellingPrice": 21.0 },
    { "shopId": 1, "fertilizerId": 5, "quantity": 20.0, "sellingPrice": 155.0 },
    { "shopId": 2, "fertilizerId": 1, "quantity": 750.0, "sellingPrice": 27.0 },
    { "shopId": 2, "fertilizerId": 2, "quantity": 0.0, "sellingPrice": 27.0 },
    { "shopId": 2, "fertilizerId": 3, "quantity": 300.0, "sellingPrice": 20.0 },
    { "shopId": 2, "fertilizerId": 6, "quantity": 400.0, "sellingPrice": 15.0 },
    { "shopId": 2, "fertilizerId": 7, "quantity": 5.0, "sellingPrice": 260.0 },
    { "shopId": 3, "fertilizerId": 1, "quantity": 320.0, "sellingPrice": 28.0 },
    { "shopId": 3, "fertilizerId": 4, "quantity": 180.0, "sellingPrice": 22.0 },
    { "shopId": 3, "fertilizerId": 5, "quantity": 0.0, "sellingPrice": 150.0 },
    { "shopId": 4, "fertilizerId": 1, "quantity": 0.0, "sellingPrice": 27.0 },
    { "shopId": 4, "fertilizerId": 2, "quantity": 260.0, "sellingPrice": 27.0 },
    { "shopId": 4, "fertilizerId": 3, "quantity": 90.0, "sellingPrice": 21.0 },
    { "shopId": 4, "fertilizerId": 6, "quantity": 120.0, "sellingPrice": 16.0 },
    { "shopId": 5, "fertilizerId": 1, "quantity": 640.0, "sellingPrice": 27.0 },
    { "shopId": 5, "fertilizerId": 4, "quantity": 90.0, "sellingPrice": 21.0 },
    { "shopId": 5, "fertilizerId": 5, "quantity": 35.0, "sellingPrice": 150.0 },
    { "shopId": 5, "fertilizerId": 7, "quantity": 0.0, "sellingPrice": 250.0 },
    { "shopId": 6, "fertilizerId": 2, "quantity": 150.0, "sellingPrice": 28.0 },
    { "shopId": 6, "fertilizerId": 3, "quantity": 210.0, "sellingPrice": 20.0 },
    { "shopId": 6, "fertilizerId": 6, "quantity": 0.0, "sellingPrice": 15.0 }
  ]
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.data.source.JsonDataSourceTest'`
Expected: PASS (5 tests).

---

### Task 4: Repository with join logic

**Files:**
- Create: `app/src/main/java/com/sarbondhu/app/data/model/ShopStock.java`, `FertilizerItem.java`, `FertilizerDetail.java`
- Create: `app/src/main/java/com/sarbondhu/app/data/repository/FertilizerRepository.java`
- Test: `app/src/test/java/com/sarbondhu/app/data/repository/FertilizerRepositoryTest.java`

**Interfaces:**
- Consumes: `DataLoader`, `JsonDataSource`, models from Task 3.
- Produces:
  - `ShopStock(Shop shop, double quantity, double sellingPrice)`; `getShop() getQuantity() getSellingPrice() isInStock()`.
  - `FertilizerItem(Fertilizer fertilizer, int shopCount)`; `getFertilizer() getShopCount()` — `shopCount` = shops with quantity > 0.
  - `FertilizerDetail(Fertilizer fertilizer, List<ShopStock> shopStocks)`; `getFertilizer() getShopStocks()`.
  - `FertilizerRepository(DataLoader)`, `static FertilizerRepository getInstance(Context)`, `List<FertilizerItem> getFertilizerItems() throws IOException`, `FertilizerDetail getFertilizerDetail(int id) throws IOException` (null if id unknown). Shop stocks sorted by quantity descending.

- [ ] **Step 1: Write the failing test**

```java
package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.source.DataLoader;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FertilizerRepositoryTest {

    private static Fertilizer fertilizer(int id) {
        return new Fertilizer(id, "সার " + id, "c", "n", "d", "u",
                Collections.emptyList(), 10.0, "কেজি");
    }

    private static Shop shop(int id) {
        return new Shop(id, "দোকান " + id, "o", "017", "জেলা", "উপজেলা", "ঠিকানা");
    }

    private static MockData sampleData() {
        return new MockData(
                Arrays.asList(fertilizer(1), fertilizer(2), fertilizer(3)),
                Arrays.asList(shop(10), shop(20), shop(30)),
                Arrays.asList(
                        new Stock(10, 1, 100.0, 27.0),
                        new Stock(20, 1, 0.0, 27.0),
                        new Stock(30, 1, 400.0, 28.0),
                        new Stock(99, 1, 999.0, 27.0),   // unknown shop → skipped
                        new Stock(10, 2, 0.0, 20.0)));   // fertilizer 2 only out of stock
    }

    private static FertilizerRepository repo(MockData data) {
        return new FertilizerRepository(() -> data);
    }

    @Test
    public void fertilizerItems_countOnlyKnownShopsWithStock() throws IOException {
        List<FertilizerItem> items = repo(sampleData()).getFertilizerItems();

        assertEquals(3, items.size());
        assertEquals(1, items.get(0).getFertilizer().getId());
        assertEquals(2, items.get(0).getShopCount());
        assertEquals(0, items.get(1).getShopCount());
        assertEquals(0, items.get(2).getShopCount());
    }

    @Test
    public void detail_shopsSortedByQuantityDescending_unknownShopSkipped() throws IOException {
        FertilizerDetail detail = repo(sampleData()).getFertilizerDetail(1);

        List<ShopStock> stocks = detail.getShopStocks();
        assertEquals(3, stocks.size());
        assertEquals(30, stocks.get(0).getShop().getId());
        assertEquals(10, stocks.get(1).getShop().getId());
        assertEquals(20, stocks.get(2).getShop().getId());
        assertTrue(stocks.get(0).isInStock());
        assertFalse(stocks.get(2).isInStock());
        assertEquals(28.0, stocks.get(0).getSellingPrice(), 0.0001);
    }

    @Test
    public void detail_fertilizerWithoutStockRowsHasEmptyList() throws IOException {
        FertilizerDetail detail = repo(sampleData()).getFertilizerDetail(3);

        assertEquals(3, detail.getFertilizer().getId());
        assertTrue(detail.getShopStocks().isEmpty());
    }

    @Test
    public void detail_unknownIdReturnsNull() throws IOException {
        assertNull(repo(sampleData()).getFertilizerDetail(42));
    }

    @Test
    public void dataIsLoadedOnlyOnce() throws IOException {
        int[] calls = {0};
        FertilizerRepository repository = new FertilizerRepository(() -> {
            calls[0]++;
            return sampleData();
        });

        repository.getFertilizerItems();
        repository.getFertilizerDetail(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void loadFailureIsNotCached_retrySucceeds() throws IOException {
        int[] calls = {0};
        DataLoader flaky = () -> {
            calls[0]++;
            if (calls[0] == 1) {
                throw new IOException("boom");
            }
            return sampleData();
        };
        FertilizerRepository repository = new FertilizerRepository(flaky);

        try {
            repository.getFertilizerItems();
            fail("expected IOException");
        } catch (IOException expected) {
            // first attempt fails
        }

        assertEquals(3, repository.getFertilizerItems().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.data.repository.FertilizerRepositoryTest'`
Expected: compilation FAIL, `cannot find symbol FertilizerRepository`.

- [ ] **Step 3: Implement the joined models**

`ShopStock.java`:
```java
package com.sarbondhu.app.data.model;

/** A shop together with its stock of one particular fertilizer. */
public class ShopStock {

    private final Shop shop;
    private final double quantity;
    private final double sellingPrice;

    public ShopStock(Shop shop, double quantity, double sellingPrice) {
        this.shop = shop;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
    }

    public Shop getShop() {
        return shop;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public boolean isInStock() {
        return quantity > 0;
    }
}
```

`FertilizerItem.java`:
```java
package com.sarbondhu.app.data.model;

/** One row of the fertilizer list. */
public class FertilizerItem {

    private final Fertilizer fertilizer;
    private final int shopCount;

    public FertilizerItem(Fertilizer fertilizer, int shopCount) {
        this.fertilizer = fertilizer;
        this.shopCount = shopCount;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    /** Number of shops that currently have this fertilizer in stock. */
    public int getShopCount() {
        return shopCount;
    }
}
```

`FertilizerDetail.java`:
```java
package com.sarbondhu.app.data.model;

import java.util.List;

/** Everything the detail screen shows. */
public class FertilizerDetail {

    private final Fertilizer fertilizer;
    private final List<ShopStock> shopStocks;

    public FertilizerDetail(Fertilizer fertilizer, List<ShopStock> shopStocks) {
        this.fertilizer = fertilizer;
        this.shopStocks = shopStocks;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public List<ShopStock> getShopStocks() {
        return shopStocks;
    }
}
```

- [ ] **Step 4: Implement the repository**

```java
package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.source.DataLoader;
import com.sarbondhu.app.data.source.JsonDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single access point for fertilizer data. Methods are blocking — call them off the main thread.
 */
public class FertilizerRepository {

    private static volatile FertilizerRepository instance;

    private final DataLoader loader;
    private MockData cache;

    public FertilizerRepository(DataLoader loader) {
        this.loader = loader;
    }

    public static FertilizerRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FertilizerRepository.class) {
                if (instance == null) {
                    instance = new FertilizerRepository(
                            new JsonDataSource(context.getApplicationContext().getAssets()));
                }
            }
        }
        return instance;
    }

    public List<FertilizerItem> getFertilizerItems() throws IOException {
        MockData data = data();
        List<FertilizerItem> items = new ArrayList<>();
        for (Fertilizer fertilizer : data.getFertilizers()) {
            int inStock = 0;
            for (ShopStock shopStock : shopStocksFor(data, fertilizer.getId())) {
                if (shopStock.isInStock()) {
                    inStock++;
                }
            }
            items.add(new FertilizerItem(fertilizer, inStock));
        }
        return items;
    }

    /** Returns null when no fertilizer has the given id. */
    public FertilizerDetail getFertilizerDetail(int fertilizerId) throws IOException {
        MockData data = data();
        for (Fertilizer fertilizer : data.getFertilizers()) {
            if (fertilizer.getId() == fertilizerId) {
                return new FertilizerDetail(fertilizer, shopStocksFor(data, fertilizerId));
            }
        }
        return null;
    }

    private synchronized MockData data() throws IOException {
        if (cache == null) {
            cache = loader.load();
        }
        return cache;
    }

    private static List<ShopStock> shopStocksFor(MockData data, int fertilizerId) {
        Map<Integer, Shop> shopsById = new HashMap<>();
        for (Shop shop : data.getShops()) {
            shopsById.put(shop.getId(), shop);
        }
        List<ShopStock> result = new ArrayList<>();
        for (Stock stock : data.getStocks()) {
            Shop shop = shopsById.get(stock.getShopId());
            if (stock.getFertilizerId() == fertilizerId && shop != null) {
                result.add(new ShopStock(shop, stock.getQuantity(), stock.getSellingPrice()));
            }
        }
        result.sort((a, b) -> Double.compare(b.getQuantity(), a.getQuantity()));
        return result;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests 'com.sarbondhu.app.data.repository.FertilizerRepositoryTest'`
Expected: PASS (6 tests).

---

### Task 5: Fertilizer list screen

**Files:**
- Create: `app/src/main/java/com/sarbondhu/app/ui/common/UiState.java`
- Create: `app/src/main/java/com/sarbondhu/app/ui/fertilizerlist/FertilizerListViewModel.java`, `FertilizerAdapter.java`
- Replace: `app/src/main/java/com/sarbondhu/app/ui/fertilizerlist/FertilizerListActivity.java`
- Create: `app/src/main/res/layout/activity_fertilizer_list.xml`, `item_fertilizer.xml`

**Interfaces:**
- Consumes: `FertilizerRepository.getInstance(Context)`, `getFertilizerItems()`, `FertilizerItem`, `BanglaNumber`, string resources from Task 1, `FertilizerDetailActivity.newIntent(Context, int)` (created in Task 6 — until then Task 5 adds it to the stub, see Step 6).
- Produces: `UiState<T>` with `Status { LOADING, SUCCESS, ERROR }`, `static loading()`, `static success(T)`, `static error(@StringRes int)`, `getStatus() getData() getMessageRes()`.

- [ ] **Step 1: UiState**

```java
package com.sarbondhu.app.ui.common;

import androidx.annotation.StringRes;

/** What a screen should show: a spinner, the data, or an error message. */
public final class UiState<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    @StringRes
    private final int messageRes;

    private UiState(Status status, T data, @StringRes int messageRes) {
        this.status = status;
        this.data = data;
        this.messageRes = messageRes;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, 0);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, 0);
    }

    public static <T> UiState<T> error(@StringRes int messageRes) {
        return new UiState<>(Status.ERROR, null, messageRes);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    @StringRes
    public int getMessageRes() {
        return messageRes;
    }
}
```

- [ ] **Step 2: ViewModel**

```java
package com.sarbondhu.app.ui.fertilizerlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.data.repository.FertilizerRepository;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FertilizerListViewModel extends AndroidViewModel {

    private final FertilizerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<List<FertilizerItem>>> state = new MutableLiveData<>();

    public FertilizerListViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
        load();
    }

    public LiveData<UiState<List<FertilizerItem>>> getState() {
        return state;
    }

    public void load() {
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                state.postValue(UiState.success(repository.getFertilizerItems()));
            } catch (IOException e) {
                state.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
```

- [ ] **Step 3: Layouts**

`app/src/main/res/layout/activity_fertilizer_list.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:attr/colorBackground"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="?attr/colorPrimary"
        android:minHeight="?attr/actionBarSize"
        app:subtitle="@string/list_subtitle"
        app:subtitleTextColor="?attr/colorOnPrimary"
        app:title="@string/list_title"
        app:titleTextColor="?attr/colorOnPrimary" />

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/fertilizer_list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:padding="12dp" />

        <ProgressBar
            android:id="@+id/progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone" />

        <TextView
            android:id="@+id/empty_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:text="@string/empty_fertilizers"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:visibility="gone" />

        <LinearLayout
            android:id="@+id/error_group"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="24dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/error_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:textAppearance="?attr/textAppearanceBodyLarge" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/retry_button"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="@string/retry" />
        </LinearLayout>
    </FrameLayout>
</LinearLayout>
```

`app/src/main/res/layout/item_fertilizer.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    android:clickable="true"
    android:focusable="true"
    app:cardCornerRadius="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceTitleMedium"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/category"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:textColor="?attr/colorPrimary" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/price"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="?attr/textAppearanceTitleSmall" />

            <TextView
                android:id="@+id/shop_count"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceBodySmall"
                android:textColor="@color/text_secondary" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 4: Adapter**

```java
package com.sarbondhu.app.ui.fertilizerlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.util.BanglaNumber;

public class FertilizerAdapter extends ListAdapter<FertilizerItem, FertilizerAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FertilizerItem item);
    }

    private static final DiffUtil.ItemCallback<FertilizerItem> DIFF =
            new DiffUtil.ItemCallback<FertilizerItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull FertilizerItem a, @NonNull FertilizerItem b) {
                    return a.getFertilizer().getId() == b.getFertilizer().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull FertilizerItem a, @NonNull FertilizerItem b) {
                    return a.getShopCount() == b.getShopCount()
                            && a.getFertilizer().getUnitPrice() == b.getFertilizer().getUnitPrice()
                            && a.getFertilizer().getName().equals(b.getFertilizer().getName());
                }
            };

    private final OnItemClickListener listener;

    public FertilizerAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fertilizer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FertilizerItem item = getItem(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView category;
        private final TextView price;
        private final TextView shopCount;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            category = itemView.findViewById(R.id.category);
            price = itemView.findViewById(R.id.price);
            shopCount = itemView.findViewById(R.id.shop_count);
        }

        void bind(FertilizerItem item) {
            Context context = itemView.getContext();
            Fertilizer fertilizer = item.getFertilizer();
            name.setText(fertilizer.getName());
            category.setText(fertilizer.getCategory());
            price.setText(context.getString(R.string.price_per_unit,
                    BanglaNumber.format(fertilizer.getUnitPrice()), fertilizer.getUnit()));
            shopCount.setText(item.getShopCount() > 0
                    ? context.getString(R.string.shop_count, BanglaNumber.format(item.getShopCount()))
                    : context.getString(R.string.not_available_anywhere));
        }
    }
}
```

- [ ] **Step 5: Activity**

```java
package com.sarbondhu.app.ui.fertilizerlist;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.ui.fertilizerdetail.FertilizerDetailActivity;

import java.util.List;

public class FertilizerListActivity extends AppCompatActivity {

    private RecyclerView list;
    private ProgressBar progress;
    private TextView emptyText;
    private View errorGroup;
    private TextView errorText;
    private FertilizerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = findViewById(R.id.fertilizer_list);
        progress = findViewById(R.id.progress);
        emptyText = findViewById(R.id.empty_text);
        errorGroup = findViewById(R.id.error_group);
        errorText = findViewById(R.id.error_text);

        adapter = new FertilizerAdapter(item -> startActivity(
                FertilizerDetailActivity.newIntent(this, item.getFertilizer().getId())));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        FertilizerListViewModel viewModel =
                new ViewModelProvider(this).get(FertilizerListViewModel.class);
        findViewById(R.id.retry_button).setOnClickListener(v -> viewModel.load());
        viewModel.getState().observe(this, this::render);
    }

    private void render(UiState<List<FertilizerItem>> state) {
        UiState.Status status = state.getStatus();
        progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            errorText.setText(state.getMessageRes());
        }

        boolean success = status == UiState.Status.SUCCESS;
        boolean empty = success && state.getData().isEmpty();
        list.setVisibility(success && !empty ? View.VISIBLE : View.GONE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (success) {
            adapter.submitList(state.getData());
        }
    }
}
```

- [ ] **Step 6: Add `newIntent` to the detail stub so this task compiles**

Replace `FertilizerDetailActivity.java` stub with:
```java
package com.sarbondhu.app.ui.fertilizerdetail;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class FertilizerDetailActivity extends AppCompatActivity {

    private static final String EXTRA_FERTILIZER_ID = "fertilizer_id";

    public static Intent newIntent(Context context, int fertilizerId) {
        return new Intent(context, FertilizerDetailActivity.class)
                .putExtra(EXTRA_FERTILIZER_ID, fertilizerId);
    }
}
```

- [ ] **Step 7: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`, all earlier tests still pass.

---

### Task 6: Fertilizer detail screen

**Files:**
- Create: `app/src/main/java/com/sarbondhu/app/ui/fertilizerdetail/FertilizerDetailViewModel.java`, `ShopStockAdapter.java`
- Replace: `app/src/main/java/com/sarbondhu/app/ui/fertilizerdetail/FertilizerDetailActivity.java`
- Create: `app/src/main/res/layout/activity_fertilizer_detail.xml`, `item_shop_stock.xml`

**Interfaces:**
- Consumes: `FertilizerRepository.getFertilizerDetail(int)`, `FertilizerDetail`, `ShopStock`, `Shop`, `UiState`, `BanglaNumber`, strings and `SectionLabel` style and `bg_badge_out` from Task 1.
- Produces: `FertilizerDetailActivity.newIntent(Context, int)` (same signature as the Task 5 stub).

- [ ] **Step 1: ViewModel**

```java
package com.sarbondhu.app.ui.fertilizerdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.repository.FertilizerRepository;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FertilizerDetailViewModel extends AndroidViewModel {

    private final FertilizerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<FertilizerDetail>> state = new MutableLiveData<>();
    private boolean started;

    public FertilizerDetailViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
    }

    public LiveData<UiState<FertilizerDetail>> getState() {
        return state;
    }

    /** Loads once; later calls (e.g. after rotation) reuse the existing state. */
    public void load(int fertilizerId) {
        if (started) {
            return;
        }
        started = true;
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                FertilizerDetail detail = repository.getFertilizerDetail(fertilizerId);
                state.postValue(detail == null
                        ? UiState.error(R.string.error_not_found)
                        : UiState.success(detail));
            } catch (IOException e) {
                state.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
```

- [ ] **Step 2: Layouts**

`app/src/main/res/layout/activity_fertilizer_detail.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:attr/colorBackground"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="?attr/colorPrimary"
        android:minHeight="?attr/actionBarSize"
        app:navigationIconTint="?attr/colorOnPrimary"
        app:titleTextColor="?attr/colorOnPrimary" />

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.core.widget.NestedScrollView
            android:id="@+id/content"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    app:cardBackgroundColor="?attr/colorPrimaryContainer"
                    app:cardCornerRadius="12dp"
                    app:strokeWidth="0dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:id="@+id/name"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:textAppearance="?attr/textAppearanceHeadlineSmall"
                            android:textColor="?attr/colorOnPrimaryContainer" />

                        <TextView
                            android:id="@+id/category"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:textAppearance="?attr/textAppearanceBodyLarge"
                            android:textColor="?attr/colorOnPrimaryContainer" />

                        <TextView
                            android:id="@+id/nutrient"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:textAppearance="?attr/textAppearanceBodyMedium"
                            android:textColor="?attr/colorOnPrimaryContainer" />

                        <TextView
                            android:id="@+id/price"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="8dp"
                            android:textAppearance="?attr/textAppearanceTitleMedium"
                            android:textColor="?attr/colorOnPrimaryContainer"
                            android:textStyle="bold" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <TextView
                    style="@style/SectionLabel"
                    android:text="@string/section_description" />

                <TextView
                    android:id="@+id/description"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textAppearance="?attr/textAppearanceBodyMedium" />

                <TextView
                    style="@style/SectionLabel"
                    android:text="@string/section_usage" />

                <TextView
                    android:id="@+id/usage"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textAppearance="?attr/textAppearanceBodyMedium" />

                <TextView
                    android:id="@+id/crops_label"
                    style="@style/SectionLabel"
                    android:text="@string/section_crops" />

                <com.google.android.material.chip.ChipGroup
                    android:id="@+id/crops_group"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />

                <TextView
                    style="@style/SectionLabel"
                    android:text="@string/section_shops" />

                <TextView
                    android:id="@+id/no_shops_text"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:paddingVertical="12dp"
                    android:text="@string/no_shops"
                    android:textAppearance="?attr/textAppearanceBodyMedium"
                    android:textColor="@color/text_secondary"
                    android:visibility="gone" />

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/shop_list"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:nestedScrollingEnabled="false" />
            </LinearLayout>
        </androidx.core.widget.NestedScrollView>

        <ProgressBar
            android:id="@+id/progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone" />

        <TextView
            android:id="@+id/error_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:padding="24dp"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:visibility="gone" />
    </FrameLayout>
</LinearLayout>
```

`app/src/main/res/layout/item_shop_stock.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="10dp"
    app:cardCornerRadius="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/shop_name"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="?attr/textAppearanceTitleMedium"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/stock_out_badge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:background="@drawable/bg_badge_out"
                android:paddingHorizontal="10dp"
                android:paddingVertical="2dp"
                android:text="@string/stock_out"
                android:textAppearance="?attr/textAppearanceLabelMedium"
                android:textColor="@color/white"
                android:visibility="gone" />
        </LinearLayout>

        <TextView
            android:id="@+id/owner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <TextView
            android:id="@+id/location"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <TextView
            android:id="@+id/address"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="@color/text_secondary" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/quantity"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="?attr/textAppearanceTitleSmall"
                android:textColor="?attr/colorPrimary" />

            <TextView
                android:id="@+id/selling_price"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceTitleSmall" />
        </LinearLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/call_button"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Shop adapter**

```java
package com.sarbondhu.app.ui.fertilizerdetail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.util.BanglaNumber;

public class ShopStockAdapter extends ListAdapter<ShopStock, ShopStockAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<ShopStock> DIFF =
            new DiffUtil.ItemCallback<ShopStock>() {
                @Override
                public boolean areItemsTheSame(@NonNull ShopStock a, @NonNull ShopStock b) {
                    return a.getShop().getId() == b.getShop().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ShopStock a, @NonNull ShopStock b) {
                    return a.getQuantity() == b.getQuantity()
                            && a.getSellingPrice() == b.getSellingPrice();
                }
            };

    private final String unit;

    public ShopStockAdapter(String unit) {
        super(DIFF);
        this.unit = unit;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), unit);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView shopName;
        private final TextView stockOutBadge;
        private final TextView owner;
        private final TextView location;
        private final TextView address;
        private final TextView quantity;
        private final TextView sellingPrice;
        private final Button callButton;

        ViewHolder(View itemView) {
            super(itemView);
            shopName = itemView.findViewById(R.id.shop_name);
            stockOutBadge = itemView.findViewById(R.id.stock_out_badge);
            owner = itemView.findViewById(R.id.owner);
            location = itemView.findViewById(R.id.location);
            address = itemView.findViewById(R.id.address);
            quantity = itemView.findViewById(R.id.quantity);
            sellingPrice = itemView.findViewById(R.id.selling_price);
            callButton = itemView.findViewById(R.id.call_button);
        }

        void bind(ShopStock shopStock, String unit) {
            Context context = itemView.getContext();
            Shop shop = shopStock.getShop();

            shopName.setText(shop.getShopName());
            owner.setText(context.getString(R.string.owner, shop.getOwnerName()));
            location.setText(context.getString(R.string.location, shop.getUpazila(), shop.getDistrict()));
            address.setText(shop.getAddress());
            sellingPrice.setText(context.getString(R.string.selling_price,
                    BanglaNumber.format(shopStock.getSellingPrice()), unit));

            boolean inStock = shopStock.isInStock();
            stockOutBadge.setVisibility(inStock ? View.GONE : View.VISIBLE);
            quantity.setVisibility(inStock ? View.VISIBLE : View.INVISIBLE);
            quantity.setText(context.getString(R.string.stock_quantity,
                    BanglaNumber.format(shopStock.getQuantity()), unit));

            String phone = shop.getPhone();
            if (phone.isEmpty()) {
                callButton.setVisibility(View.GONE);
            } else {
                callButton.setVisibility(View.VISIBLE);
                callButton.setText(context.getString(R.string.call, BanglaNumber.toBangla(phone)));
                callButton.setOnClickListener(v -> {
                    try {
                        context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                    } catch (ActivityNotFoundException ignored) {
                        // Device has no dialer (e.g. tablet); nothing to do.
                    }
                });
            }
        }
    }
}
```

- [ ] **Step 4: Activity**

```java
package com.sarbondhu.app.ui.fertilizerdetail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

public class FertilizerDetailActivity extends AppCompatActivity {

    private static final String EXTRA_FERTILIZER_ID = "fertilizer_id";

    private MaterialToolbar toolbar;
    private View content;
    private ProgressBar progress;
    private TextView errorText;
    private TextView name;
    private TextView category;
    private TextView nutrient;
    private TextView price;
    private TextView description;
    private TextView usage;
    private TextView cropsLabel;
    private ChipGroup cropsGroup;
    private TextView noShopsText;
    private RecyclerView shopList;

    public static Intent newIntent(Context context, int fertilizerId) {
        return new Intent(context, FertilizerDetailActivity.class)
                .putExtra(EXTRA_FERTILIZER_ID, fertilizerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_detail);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        content = findViewById(R.id.content);
        progress = findViewById(R.id.progress);
        errorText = findViewById(R.id.error_text);
        name = findViewById(R.id.name);
        category = findViewById(R.id.category);
        nutrient = findViewById(R.id.nutrient);
        price = findViewById(R.id.price);
        description = findViewById(R.id.description);
        usage = findViewById(R.id.usage);
        cropsLabel = findViewById(R.id.crops_label);
        cropsGroup = findViewById(R.id.crops_group);
        noShopsText = findViewById(R.id.no_shops_text);
        shopList = findViewById(R.id.shop_list);
        shopList.setLayoutManager(new LinearLayoutManager(this));

        FertilizerDetailViewModel viewModel =
                new ViewModelProvider(this).get(FertilizerDetailViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.load(getIntent().getIntExtra(EXTRA_FERTILIZER_ID, -1));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render(UiState<FertilizerDetail> state) {
        UiState.Status status = state.getStatus();
        progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        errorText.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            errorText.setText(state.getMessageRes());
        } else if (status == UiState.Status.SUCCESS) {
            bind(state.getData());
        }
    }

    private void bind(FertilizerDetail detail) {
        Fertilizer fertilizer = detail.getFertilizer();
        toolbar.setTitle(fertilizer.getName());
        name.setText(fertilizer.getName());
        category.setText(fertilizer.getCategory());
        nutrient.setText(getString(R.string.nutrient, BanglaNumber.toBangla(fertilizer.getNpkRatio())));
        price.setText(getString(R.string.fixed_price,
                BanglaNumber.format(fertilizer.getUnitPrice()), fertilizer.getUnit()));
        description.setText(fertilizer.getDescription());
        usage.setText(fertilizer.getUsage());

        cropsGroup.removeAllViews();
        for (String crop : fertilizer.getRecommendedCrops()) {
            Chip chip = new Chip(this);
            chip.setText(crop);
            chip.setClickable(false);
            cropsGroup.addView(chip);
        }
        boolean hasCrops = !fertilizer.getRecommendedCrops().isEmpty();
        cropsLabel.setVisibility(hasCrops ? View.VISIBLE : View.GONE);
        cropsGroup.setVisibility(hasCrops ? View.VISIBLE : View.GONE);

        boolean hasShops = !detail.getShopStocks().isEmpty();
        noShopsText.setVisibility(hasShops ? View.GONE : View.VISIBLE);
        shopList.setVisibility(hasShops ? View.VISIBLE : View.GONE);
        ShopStockAdapter adapter = new ShopStockAdapter(fertilizer.getUnit());
        shopList.setAdapter(adapter);
        adapter.submitList(detail.getShopStocks());
    }
}
```

- [ ] **Step 5: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`, all tests pass.

---

### Task 7: End-to-end check on an emulator

**Files:** none (verification only).

- [ ] **Step 1: Full test + build**

Run: `./gradlew clean testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`; 19 tests pass.

- [ ] **Step 2: Install and launch on an emulator**

```bash
~/Library/Android/sdk/emulator/emulator -list-avds          # pick one
~/Library/Android/sdk/emulator/emulator -avd <AVD> -no-snapshot-save &   # run in background
~/Library/Android/sdk/platform-tools/adb wait-for-device
./gradlew installDebug
~/Library/Android/sdk/platform-tools/adb shell am start -n com.sarbondhu.app/.ui.fertilizerlist.FertilizerListActivity
```
If no AVD exists, report that and skip to Step 4.

- [ ] **Step 3: Check screens via screenshots** (`adb exec-out screencap -p > shot.png` into the scratchpad, then view)

- List shows 8 Bangla fertilizer cards, prices in Bangla digits, ইউরিয়া says "৪টি দোকানে পাওয়া যাচ্ছে", জৈব সার says "কোনো দোকানে স্টক নেই".
- Tap ইউরিয়া: header, description, usage, crop chips, 5 shop cards sorted by quantity (৭৫০ first), সবুজ বাংলা কৃষি বিতান shows "স্টক নেই" badge.
- Rotate (`adb shell settings put system user_rotation 1` with auto-rotate off) — content stays, no spinner flash.
- Back arrow returns to the list. Tap জৈব সার → "কোনো দোকানে এই সার পাওয়া যায়নি".

- [ ] **Step 4: Report** results to the user honestly, including anything not verified.
