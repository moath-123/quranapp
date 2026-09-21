# Changelog

## 1.2.0 — 2026-09-21

Pilot readiness (phase 5 tooling).

### iOS SDK (`TaahudQuranSDK`)
- `QuranConfig.onEvent` / `QuranEvent`: data load and page-install events (start, retry, success with duration/bytes/attempts, failure with error code) for pilot metrics.
- `run-tests.sh` builds outside iCloud-synced folders (fixes ad-hoc signing of the test bundle).

### Example app
- Remote app config (`new_mushaf` flag + page-image settings) with cached fallback; legacy mushaf when the flag is off.
- About screen with Quran.com attribution, server settings and the SDK event log.
- Fixed: selected-ayah card lingered after changing pages; download errors now show a clear Arabic message.

### Server (`server/laravel-kit`, new)
- `GET /api/quran/app-config`: kill switch, stable percentage rollout, tester allowlist, page-image settings.
- Wird sync (upsert by `client_id`, synced deletes, per-timezone stats), bookmarks, reading position — ayahs by id 1–6236.
- Idempotent `install.sh`; 15 feature tests run in CI on a fresh Laravel app (PHP 8.3/8.4).

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
