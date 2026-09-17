# Quran SDK (`quran-sdk`)

Android library for integrating Quran text data and mushaf page rendering into **any client app**.

The SDK provides:
- **Data APIs** — surahs, juz, search, ayahs by page/surah/juz
- **Embeddable page UI** — `QuranPageView` / `QuranPageFragment` (rendering only)
- **Ayah interaction callbacks** — tap / long-press (no built-in menus)

The SDK does **not** provide:
- Navigation, tabs, or app shell UI
- Search screen UI
- Bookmark storage (client app responsibility)
- Popups / context menus on ayah selection

---

## Requirements

| Item | Version |
|------|---------|
| minSdk | 24 |
| compileSdk | 36 |
| Kotlin | 2.0+ |
| Java | 11 |

---

## Integration

### Option A — Local module (same project)

In `settings.gradle.kts`:

```kotlin
include(":quran-sdk")
```

In your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":quran-sdk"))
}
```

### Option B — AAR file

Build the release AAR:

```bash
./gradlew :quran-sdk:assembleRelease
```

Output:

```
quran-sdk/build/outputs/aar/quran-sdk-release.aar
```

Copy to your project `app/libs/` and add:

```kotlin
dependencies {
    implementation(files("libs/quran-sdk-release.aar"))
    // Transitive dependencies required by the SDK:
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
}
```

### Option C — Maven Local (for team distribution)

```bash
./gradlew :quran-sdk:publishToMavenLocal
```

In client `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
```

In app `build.gradle.kts`:

```kotlin
implementation("com.codesteem:quran-sdk:1.1.0")
```

---

## Quick start

### 1. Initialize (required once)

```kotlin
import com.codesteem.quransdk.QuranSdk
    import kotlinx.coroutines.launch

class MyActivity : AppCompatActivity() {

    private val quran = QuranSdk.create(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            quran.initialize() // loads quran.json into local DB on first run
        }
    }
}
```

### 2. Data APIs

```kotlin
// Lists
val surahs = quran.getSurahs()       // 114 surahs
val juzList = quran.getJuzList()     // 30 juz

// Search (returns structured data — you build the UI)
val results = quran.search("الرحمن")

// Ayahs
val pageAyahs = quran.getAyahsByPage(1)
val surahAyahs = quran.getAyahsBySurah(1)
val juzAyahs = quran.getAyahsByJuz(1)
val ayah = quran.getAyahById(123)
val anchor = quran.getFirstAyahOnPage(15)  // for page jump
val maxPage = quran.getMaxPage()           // usually 604
```

### 3. Embeddable page view (XML)

```xml
<com.codesteem.quransdk.ui.QuranPageView
    android:id="@+id/quranPageView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val pageView = findViewById<QuranPageView>(R.id.quranPageView)

lifecycleScope.launch {
    quran.initialize()
    pageView.bind(quran)
    pageView.setPageListener(object : QuranPageListener {
        override fun onAyahTapped(ayah: Ayah) {
            // client shows its own UI
        }
        override fun onAyahLongPressed(ayah: Ayah, screenX: Float, screenY: Float) {
            // client shows its own popup/menu at (screenX, screenY)
        }
    })
    pageView.goToPage(1)
}
```

### 4. Embeddable page view (Fragment — recommended)

```kotlin
val pageFragment = QuranSdk.createPageFragment().apply {
    quranApi = quran
    pageListener = object : QuranPageListener {
        override fun onAyahTapped(ayah: Ayah) { /* ... */ }
        override fun onAyahLongPressed(ayah: Ayah, screenX: Float, screenY: Float) { /* ... */ }
    }
}

supportFragmentManager.beginTransaction()
    .replace(R.id.container, pageFragment)
    .commit()

lifecycleScope.launch {
    quran.initialize()
    pageFragment.goToPage(1)
}
```

### 5. Bookmarks (client-side only)

The SDK has **no bookmark API**. The client app should:

1. Receive ayah data from `QuranPageListener` or `getAyahById()`
2. Save to its own database / SharedPreferences / Room
3. Build its own bookmark list UI

See the demo app package `com.codesteem.quranapp.sdktest` for a reference implementation (`ClientBookmarkStore`).

---

## Public API reference

### `QuranSdk`

| Method | Description |
|--------|-------------|
| `create(context)` | Returns `QuranApi` instance |
| `createPageView(context)` | Returns embeddable `QuranPageView` |
| `createPageFragment()` | Returns `QuranPageFragment` |

### `QuranApi`

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | Unit | Import Quran JSON to local DB (first run) |
| `getSurahs()` | `List<Surah>` | All 114 surahs |
| `getJuzList()` | `List<Juz>` | All 30 juz |
| `search(query, limit)` | `List<SearchResult>` | Arabic text search |
| `getAyahsByPage(page)` | `List<Ayah>` | Ayahs on a mushaf page |
| `getAyahsBySurah(number)` | `List<Ayah>` | All ayahs in a surah |
| `getAyahsByJuz(number)` | `List<Ayah>` | All ayahs in a juz |
| `getAyahById(id)` | `Ayah?` | Single ayah |
| `getFirstAyahOnPage(page)` | `Ayah?` | Anchor ayah for page jump |
| `getMaxPage()` | `Int` | Total pages (604) |

### Models

**`Surah`** — `number`, `nameAr`, `nameEn`, `ayahCount`, `startPage`

**`Juz`** — `number`, `nameAr`, `nameEn`, `startPage`

**`Ayah`** — `id`, `surahNumber`, `ayahNumber`, `juzNumber`, `pageNumber`, `textUthmani`, `textSimple`, `lineStart`, `lineEnd`

**`SearchResult`** — `ayahId`, `ayahNumber`, `surahNumber`, `surahNameAr/En`, `textUthmani`, `textSimple`, `pageNumber`, `juzNumber`, `juzNameAr/En`

### `QuranPageListener`

| Callback | When |
|----------|------|
| `onAyahTapped(ayah)` | Single tap on ayah text |
| `onAyahLongPressed(ayah, x, y)` | Long press — use screen coords for client popup |

---

## Architecture

```
quran-sdk/
├── api/           ← Public: QuranApi, models, QuranPageListener
├── ui/            ← Public: QuranPageView, QuranPageFragment
├── internal/      ← Private: Room DB, page builder, adapters
└── assets/
    └── quran.json ← Bundled Quran data
```

---

## Demo app

This repository includes a reference client in the `app` module:

| Screen | Purpose |
|--------|---------|
| `SdkApiTestActivity` | Test all data APIs + client bookmark demo |
| Main app (`HomePageActivity`) | Legacy standalone app (not SDK-based yet) |

Run debug build — launcher **"SDK API Test"** opens the integration demo.

---

## ProGuard

The SDK ships `consumer-rules.pro`. If you use R8/ProGuard, ensure Room and SDK models are kept (rules included in AAR).

---

## Version

**1.1.0** — Data fixes, atomic versioned import, juz start pages fixed, tests. See `CHANGELOG.md` at the repository root.

**1.0.0** — Initial SDK release with data APIs and embeddable page view.

---

## Support

Package: `com.codesteem.quransdk`  
Artifact: `com.codesteem:quran-sdk:1.1.0`
