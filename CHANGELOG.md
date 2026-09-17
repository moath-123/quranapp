# Changelog

## 1.1.0 — 2026-09-17

Readiness work before adopting the SDK in Taahud (see `docs/readiness-plan.md`).

### Data (`quran.json`, shared by Android, iOS and the web demo)
- Ayah pages aligned to the Madani 1405 edition used by the page images (56 ayahs were on 1440-edition pages).
- `line_start` / `line_end` recomputed from `ayahinfo_1024.db` (360 ayahs had 0).
- Al-Fatiha start page 2 → 1; juz 11 start page 201 → 202 in the top-level list.
- Stray U+2009 removed from Al-Baqarah 72.
- New `tools/validate_data.py` (runs in CI) checks the data against `quran.ar.uthmani.db`, `ayahinfo_1024.db` and quran-ios reference arrays.

### Android SDK (`com.codesteem:quran-sdk`)
- **Fixed:** `getJuzList()` returned wrong start pages for 28 of 30 juz.
- **Fixed:** `QuranPageView.goToPage()` right after `bind()` was ignored.
- **Fixed:** build failed on machines with an Arabic system locale (Room/KSP generated Arabic-Indic digits).
- Import is now one transaction with a data version (`DATA_VERSION = 2`), re-imports incomplete tables, and is safe to call concurrently.
- Invalid bundled data throws the new public `InvalidQuranDataException` instead of importing partial data.
- New `QuranPageView.currentPage`.
- Demo app `minSdk` 35 → 24.
- 20 unit tests and 17 instrumented tests (API 24 and API 35), run in CI.

### iOS SDK (`TaahudQuranSDK`, new)
- Swift package replacing `QuranSDK.xcframework`, which could not be imported (no Swift module).
- `QuranAPI` mirrors the Android API; same data and search behaviour.
- Mushaf page images: verified, atomic, versioned installation (`ensurePagesInstalled`), optional SHA-256 pinning.
- Ayah highlight rectangles and tap hit-testing from `ayahinfo_1024.db`.
- SwiftUI `QuranPageView` (RTL paging, highlights, tap / long-press).
- 29 Swift Testing tests (Command Line Tools or Xcode), plus opt-in network and packaged-archive tests; CI builds for iOS and tests on an iPhone simulator.

### Tooling
- `tools/package_pages.sh`: reproducible page-image archive + SHA256SUMS + manifest for self-hosting.
- `tools/setup_android_env.sh`: JDK 17 + Android SDK + emulators without sudo.

## 1.0.0
- Initial SDK release (Android) and binary iOS framework.
