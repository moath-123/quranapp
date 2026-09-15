# QuranSDK.xcframework (binary distribution)

## Build (maintainers)

From repo root:

```bash
./QuranSDKPackage/scripts/build-xcframework.sh
```

Outputs:

| Artifact | Path |
|----------|------|
| XCFramework | `QuranSDKPackage/build/xcframework/QuranSDK.xcframework` |
| Zip | `QuranSDK.xcframework.zip` (repo root) |
| Resource bundle | `QuranSDKPackage/build/xcframework/QuranSDK_QuranSDK.bundle` |
| Zip + bundle | `QuranSDK.xcframework-full.zip` (repo root, if bundle staged) |

Build time ~30–60 seconds (device + simulator archive).

## Client integration (Xcode)

1. Drag **`QuranSDK.xcframework`** into the host project (copy if needed).
2. Target → **General → Frameworks, Libraries, and Embedded Content** → add `QuranSDK.xcframework` → **Embed & Sign**.
3. Drag **`QuranSDK_QuranSDK.bundle`** from `QuranSDKPackage/build/xcframework/` (same build) into the app target → **Copy Bundle Resources**.
4. In Swift files: `import QuranSDK`

```swift
import QuranSDK

try await QuranAPI.ensureInstalled { pct, msg in }
let surahs = QuranAPI.getSurahs()

QuranPageView(page: $page, onAyahSelection: { selection in
    // host menu
})
```

## SPM vs XCFramework

| Method | When to use |
|--------|-------------|
| **Swift Package** (`QuranSDKPackage/`) | Preferred — source, easy updates |
| **XCFramework** | Client wants binary only, no SPM |

## Notes

- Static API namespace is **`QuranAPI`** (`QuranAPI.getSurahs()`, `QuranAPI.ensureInstalled()`). The Swift **module** is still `QuranSDK` (`import QuranSDK`). This avoids a module/enum name clash when building the XCFramework.
- Static XCFramework (includes ZIPFoundation).
- iOS **15.0+**.
- First run still downloads mushaf page images via `ensureInstalled`.
- Legacy type names work: `QuranPagesView`, `QuranSurahInfo`, etc.
