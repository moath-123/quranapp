# تصميم نسخة iOS — `TaahudQuranSDK`

## الهدف
حزمة Swift (SPM) تحل محل `QuranSDK.xcframework` المعطوب (F1)، بواجهة مطابقة لـ`QuranApi` في أندرويد، وبنفس البيانات المفحوصة.

## البنية

```
ios/TaahudQuranSDK/
├── Package.swift                 iOS 15+ (ومنصة macOS 12 لتشغيل الاختبارات على الجهاز بدون Xcode)
├── Sources/TaahudQuranCore/      Foundation + SQLite3 + CryptoKit — بلا واجهات
│   ├── QuranAPI.swift            نقطة الدخول (مطابقة لـ QuranApi في أندرويد)
│   ├── Models.swift              Surah, Juz, Ayah, SearchResult, AyahRect
│   ├── QuranDataStore.swift      قراءة quran.json والتحقق منه (نفس قواعد QuranDataMapper)
│   ├── ArabicNormalizer.swift    نسخة مطابقة لـ ArabicNormalizer.kt
│   ├── QuranSearch.swift         نفس خوارزمية البحث في أندرويد
│   ├── PageGeometry.swift        مستطيلات الآيات من ayahinfo_1024.db + تحديد الآية عند النقر
│   ├── PagesInstaller.swift      تنزيل صور الصفحات والتحقق منها وتثبيتها بشكل ذري
│   ├── QuranConfig.swift         الإعدادات
│   └── Resources/                quran.json + ayahinfo_1024.db (نسخ من المصدر الموحّد، ويفحص CI تطابقها)
├── Sources/TaahudQuranUI/        SwiftUI (iOS فقط)
│   └── QuranPageView.swift       صفحات مصحف المدينة + طبقة التظليل + onAyahTap / onAyahLongPress
├── Tests/TaahudQuranCoreTests/   Swift Testing — تعمل بـ `swift test` على macOS
└── Example/                      تطبيق مثال (بعد تثبيت Xcode)
```

## الواجهة (مقابل أندرويد)

| أندرويد `QuranApi` | iOS `QuranAPI` |
|---|---|
| `suspend fun initialize()` | `func initialize() async throws` |
| `getSurahs()` | `func getSurahs() async throws -> [Surah]` |
| `getJuzList()` | `func getJuzList() async throws -> [Juz]` |
| `search(query, limit = 200)` | `func search(_ query: String, limit: Int = 200) async throws -> [SearchResult]` |
| `getAyahsByPage(page)` | `func getAyahsByPage(_ page: Int) async throws -> [Ayah]` |
| `getAyahsBySurah(n)` | `func getAyahsBySurah(_ number: Int) async throws -> [Ayah]` |
| `getAyahsByJuz(n)` | `func getAyahsByJuz(_ number: Int) async throws -> [Ayah]` |
| `getAyahById(id)` | `func getAyahById(_ id: Int) async throws -> Ayah?` |
| `getFirstAyahOnPage(page)` | `func getFirstAyahOnPage(_ page: Int) async throws -> Ayah?` |
| `getMaxPage()` | `func getMaxPage() async throws -> Int` |
| `QuranPageView` + `QuranPageListener` | `QuranPageView(api:page:onAyahTap:onAyahLongPress:)` |
| — (أندرويد يعرض نصاً حالياً) | `func ensurePagesInstalled(progress:) async throws` + `pageImageURL(_:)` + `ayahRects(page:)` + `ayahAt(page:x:y:)` |
| `InvalidQuranDataException` | `QuranError.invalidData(String)` |

النماذج بنفس الحقول والأسماء: `Surah(number, nameAr, nameEn, ayahCount, startPage)`، و`Ayah(id, surahNumber, ayahNumber, juzNumber, pageNumber, textUthmani, textSimple, lineStart, lineEnd)`، وهكذا.

## الإعدادات — `QuranConfig`
| الخاصية | الافتراضي | ملاحظة |
|---|---|---|
| `pagesArchiveURL` | `https://files.quran.app/hafs/madani/zips/images_1024.zip` | يُستبدل بخادم تعاهد (D2) |
| `pagesArchiveSHA256` | `nil` | عند تحديده يُرفض أي ملف لا يطابق |
| `storageDirectory` | `Application Support/TaahudQuran` | مستثنى من iCloud backup |
| `pagesVersion` | `1` | يُرفع عند تغيّر حزمة الصور لإعادة التنزيل |
| `downloadRetries` | `3` | مع تأخير متزايد |

## تثبيت الصور — `PagesInstaller`
أخذنا نمط quran-ios في التحقق من التنزيل (ملف نجاح مرتبط بنسخة)، مع إضافات:
1. التنزيل إلى ملف مؤقت، مع إعادة المحاولة عند فشل الشبكة.
2. التحقق من SHA-256 عند تحديده.
3. فك الضغط في مجلد مؤقت، والتأكد من وجود الصفحات الـ604 كاملة (`pageNNN.png`).
4. نقل المجلد إلى مكانه النهائي دفعة واحدة، ثم كتابة `installed-v{N}`.
5. أي فشل يحذف الملفات المؤقتة ولا يمس التثبيت السابق.

## القرارات الخاصة بـiOS
- **لا نعتمد على حزم quran-ios مباشرة (تفصيل لـ D1):** حزمة `QuranEngine` معرّفة لـiOS فقط، وتجلب GRDB و swift-log وغيرها، وخدمة الصور فيها تعتمد على UIKit. لذلك نبني جوهراً صغيراً بلا اعتماديات ثقيلة، يُختبر الآن بدون Xcode.
  - نستفيد من quran-ios كمرجع للبيانات: `tools/reference` يُفحص في CI.
  - ونستفيد منه كنمط تصميم: ملف النجاح المرتبط بالنسخة.
  - لو احتجنا لاحقاً تلاوة أو ترجمة أو طبعات أخرى، يمكن إضافة حزم quran-ios المناسبة.
- **اعتمادية خارجية واحدة:** [ZIPFoundation](https://github.com/weichsel/ZIPFoundation) (MIT) لفك الضغط، لأن iOS لا يوفر أداة zip. هي نفس المكتبة التي كانت داخل الملف القديم.
- **مصدر بيانات واحد:** `quran.json` المفحوص نفسه في أندرويد و iOS والنموذج، و`tools/sync_ios_resources.sh` ينسخه، والـCI يتأكد من التطابق.
- **المفضلة والورد خارج الـSDK (D5):** الـSDK توفر `onAyahTap` و `onAyahLongPress` فقط.

## متطلبات الترخيص
- **quran-ios (Apache-2.0):** نذكر المصدر في `NOTICE` (البيانات المرجعية في `tools/reference` مأخوذة منه).
- **ZIPFoundation (MIT):** نذكرها في `NOTICE`.
- **صور الصفحات و `ayahinfo_1024.db`:** مصدرها Quran.com (منشورة للعامة)، مع ذكر المصدر داخل التطبيق.

## خطة الاختبار
| ما يُختبر | أين | متى |
|---|---|---|
| البيانات، البحث، التطبيع، المستطيلات، التثبيت (ملف zip محلي) | `swift test` على macOS | ✅ الآن (المرحلة ٣أ) |
| بناء الحزمة لـiOS + واجهة SwiftUI | `xcodebuild` على محاكي iOS 15 وآخر إصدار | بعد تثبيت Xcode (٣ب) |
| التطبيق المثال: التقليب، النقر، التظليل، أول تثبيت بدون إنترنت، انقطاع التنزيل | محاكي iOS + أداة iOS Simulator | ٣ب |
| الذاكرة أثناء التقليب | Instruments / `xcodebuild test` | ٣ب |
