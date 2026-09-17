# دليل دمج `TaahudQuranSDK` في تطبيق iOS (Swift)

المتطلبات: iOS 15+، Xcode 16+ (Swift 6).

## 1. إضافة الحزمة
SPM يتطلب أن يكون `Package.swift` في جذر المستودع، والحزمة حالياً داخل `ios/TaahudQuranSDK`. لذلك:
- **الآن (تجربة):** Xcode ← File ← Add Package Dependencies ← Add Local ← مجلد `ios/TaahudQuranSDK`.
- **عند الاعتماد:** نقل المجلد إلى مستودع مستقل (مثل `taahud-quran-sdk`) مع tag للإصدار، ثم:
```swift
.package(url: "https://github.com/<org>/taahud-quran-sdk.git", from: "1.1.0")
```

## 2. الإعداد
```swift
import TaahudQuranCore
import TaahudQuranUI

let quran = QuranAPI(config: QuranConfig(
    // خادم تعاهد (قرار D2) — القيم من dist/pages/pages-manifest.json
    pagesArchiveURL: URL(string: "https://cdn.taahud.example/quran/images_1024-v1.zip")!,
    pagesArchiveSHA256: "882e5c7bc8d43a4efac679e7c85b64e1fc059783dab6babc02365f2c36a286cb",
    pagesVersion: 1
))
```
- **`QuranAPI` كائن واحد للتطبيق** (actor)، يُنشأ مرة ويُمرَّر.
- **بدون إعدادات:** يعمل على رابط Quran.com مباشرة.

## 3. البيانات
```swift
try await quran.initialize()              // يقرأ البيانات المضمّنة ويتحقق منها (آمن للتكرار)
let surahs = try await quran.getSurahs()   // 114
let juz = try await quran.getJuzList()     // 30 مع صفحات البداية الصحيحة
let hits = try await quran.search("الرحمن")
let page = try await quran.getAyahsByPage(50)
```
عند خلل في البيانات المضمّنة يُرمى `QuranError.invalidData`. هذا يُكتشف في الاختبارات قبل النشر، والفحص موجود في CI.

## 4. صور الصفحات (أول تشغيل)
```swift
if !quran.arePagesInstalled {
    try await quran.ensurePagesInstalled { progress in
        switch progress {
        case .downloading(let fraction): print("تنزيل \(Int(fraction * 100))٪")
        case .verifying, .extracting: print("تجهيز…")
        case .installed: print("جاهز")
        }
    }
}
```
- **الأخطاء المحتملة:** `QuranError.downloadFailed`، `.checksumMismatch`، `.incompleteArchive`. كلها تترك الجهاز نظيفاً وتسمح بإعادة المحاولة.
- **التوصية:** شاشة تنزيل واضحة مع زر «إعادة المحاولة»، مع الإبقاء على مصحف تعاهد الحالي أثناء ذلك.

## 5. عرض المصحف
```swift
struct MushafScreen: View {
    let quran: QuranAPI
    @State private var page = 1
    @State private var selected: Set<Int> = []

    var body: some View {
        QuranPageView(
            api: quran,
            page: $page,
            highlightedAyahIds: selected,
            onAyahTap: { ayah in selected = [ayah.id] /* قائمة التطبيق: ورد، مفضلة، نسخ… */ },
            onAyahLongPress: { ayah in /* قائمة مطوّلة */ },
            onEmptyTap: { /* إظهار/إخفاء الأشرطة (وضع التركيز) */ }
        )
        .ignoresSafeArea()
    }
}
```
- **التقليب:** من اليمين لليسار مثل المصحف الورقي (السحب لليمين = الصفحة التالية).
- **التظليل:** `highlightedAyahIds` تتحكم به بالكامل من التطبيق، مثل الآية المختارة أو نطاق الورد (`Set(from...to)`).
- **الوضع الليلي:** يُطبَّق تلقائياً حسب `colorScheme`.

## 6. التشغيل خلف مفتاح (مع الرجوع للمصحف الحالي)
```swift
@ViewBuilder
func mushafView() -> some View {
    if FeatureFlags.newMushaf && quran.arePagesInstalled {
        MushafScreen(quran: quran)
    } else {
        LegacyMushafView()   // مصحف تعاهد الحالي
    }
}
```
- **قيمة المفتاح:** تأتي من الخادم (Laravel)، ليمكن إيقافه فوراً بدون تحديث التطبيق.
- **التنزيل في الخلفية:** يبدأ فور تفعيل المفتاح. لا يُعرض المصحف الجديد إلا بعد اكتمال التثبيت.

## 7. الورد والمفضلة (من جهة التطبيق — قرار D5)
- تُخزَّن بمعرّفات الآيات (`ayah.id` من 1 إلى 6236)، لا بأرقام الصفحات، فتبقى صحيحة مع أي تغيير في الطبعة.
- المزامنة مع الحساب عبر [واجهة Laravel المقترحة](laravel-wird-api.md).

## 8. الاختبار
```bash
cd ios/TaahudQuranSDK
./run-tests.sh                        # بدون Xcode (Command Line Tools)
swift test                            # مع Xcode
TAAHUD_NETWORK_TESTS=1 ./run-tests.sh --filter RealArchiveTests   # تنزيل حقيقي
```
