# Quran App & SDK

تطبيق قرآن + SDK لأندرويد و iOS، ومعه نموذج تجريبي يشتغل في المتصفح.

## المحتويات

| المجلد | الوصف |
|--------|-------|
| `quranapp-main/` | مشروع أندرويد: مكتبة `quran-sdk` (بيانات القرآن + عرض الصفحات) وتطبيق تجريبي (`SdkApiTestActivity`) |
| `iOS-SDK/` | نسخة iOS الثنائية: `QuranSDK.xcframework.zip` + حزمة الموارد `QuranSDK_QuranSDK.bundle` (قواعد النص و `ayahinfo_1024.db`) |
| `web-demo/` | نموذج ويب مصغّر يحاكي واجهة `QuranApi` لتجربة المزايا بدون Xcode أو Android Studio |

## النموذج التجريبي (web-demo)

- عرض صفحات **مصحف المدينة** (٦٠٤ صفحة) مع الضغط على الآية وتظليلها باستخدام إحداثيات `ayahinfo_1024.db`
- وضع نصي بالرسم العثماني (مثل Android SDK)
- فهرس السور والأجزاء، البحث بدون تشكيل أو برقم الصفحة، المفضلة
- مختبر لدوال `QuranApi` (`getSurahs`، `search`، `getAyahsByPage`، …)

تشغيله محلياً:

```bash
node web-demo/serve.js
```

ثم افتح http://localhost:8765

## ملاحظات

- صور الصفحات في `web-demo/pages/` مصدرها Quran.com (`files.quran.app/hafs/madani/zips/images_1024.zip`) — نفس المصدر اللي يستخدمه iOS SDK عند أول تشغيل.
- في `quran.json` سورة الفاتحة `page_number = 2` والصحيح `1`، فـ `getSurahs()` في Android SDK يرجّع صفحة بداية غلط للفاتحة.
