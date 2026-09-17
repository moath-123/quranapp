# دليل دمج Android SDK (`com.codesteem:quran-sdk`)

المتطلبات: minSdk 24، JDK 17 للبناء.

## 1. الإضافة
مشروع واحد:
```kotlin
// settings.gradle.kts
include(":quran-sdk")
// app/build.gradle.kts
implementation(project(":quran-sdk"))
```
أو كحزمة Maven محلية (`bash gradlew :quran-sdk:publishToMavenLocal`):
```kotlin
implementation("com.codesteem:quran-sdk:1.1.0")
```

> **جهاز لغته عربية؟** `gradle.properties` في المشروع يثبّت لغة البناء على الإنجليزية، وإلا فشل Room/KSP. انسخ نفس السطرين لمشروعك (انظر F13).

## 2. البيانات
```kotlin
val quran = QuranSdk.create(context)
lifecycleScope.launch {
    try {
        quran.initialize()   // استيراد ذري + نسخة بيانات؛ آمن للتكرار ومن عدة أماكن
    } catch (e: InvalidQuranDataException) {
        // البيانات المضمّنة غير صالحة — لم يُستورد شيء، والبيانات السابقة سليمة
    }
    val juz = quran.getJuzList()          // صفحات البداية صحيحة (1، 22، 42 … 582)
    val hits = quran.search("الرحمن")
}
```

## 3. عرض الصفحات
```kotlin
pageView.setPageListener(listener)
pageView.bind(quran)
pageView.goToPage(50)      // يعمل حتى لو استُدعي مباشرة بعد bind()
pageView.currentPage       // الصفحة الحالية
```
**العرض الحالي في أندرويد نص عثماني.** توحيده على صور مصحف المدينة مثل iOS (قرار D4) عمل مستقل مقترح:
- عارض صور + طبقة تظليل من `ayahinfo_1024.db`.
- المنطق نفسه موجود في `web-demo/app.js` وفي `PageGeometry.swift`.

## 4. التخصيص
الألوان والمقاسات موارد عادية، يمكن تجاوزها بنفس الأسماء في التطبيق:
```xml
<!-- app/src/main/res/values/colors.xml -->
<color name="quransdk_header">#9B5A2C</color>
<color name="quransdk_highlight">#66FFD54F</color>
<color name="quransdk_page_bg">#FFFCF5</color>
```
والمقاسات في `quransdk_dimens.xml` (`quransdk_ayah_text` وغيره).

## 5. تحديث البيانات
عند تعديل `quran.json`:
1. `python3 tools/validate_data.py` يجب أن ينجح.
2. رفع `QuranImporter.DATA_VERSION`، فيُعاد الاستيراد تلقائياً عند المستخدمين الحاليين.
3. `bash tools/sync_ios_resources.sh` لنسخ الملف إلى iOS.

## 6. الاختبار
```bash
cd quranapp-main
bash gradlew :quran-sdk:testDebugUnitTest :quran-sdk:lintDebug
bash gradlew :quran-sdk:connectedDebugAndroidTest   # مع محاكي أو جهاز متصل
```
