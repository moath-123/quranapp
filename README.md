# Quran App & SDK

مكتبة مصحف لأندرويد و iOS، مع نموذج تجريبي يعمل في المتصفح، وخطة جاهزية قبل اعتمادها في تطبيق تعاهد.

[![Data integrity](https://github.com/moath-123/quranapp/actions/workflows/data.yml/badge.svg)](https://github.com/moath-123/quranapp/actions/workflows/data.yml)
[![Android SDK](https://github.com/moath-123/quranapp/actions/workflows/android.yml/badge.svg)](https://github.com/moath-123/quranapp/actions/workflows/android.yml)
[![Laravel kit](https://github.com/moath-123/quranapp/actions/workflows/laravel-kit.yml/badge.svg)](https://github.com/moath-123/quranapp/actions/workflows/laravel-kit.yml)
[![iOS SDK](https://github.com/moath-123/quranapp/actions/workflows/ios.yml/badge.svg)](https://github.com/moath-123/quranapp/actions/workflows/ios.yml)

**النموذج التجريبي:** https://moath-123.github.io/quranapp/

## المحتويات

| المجلد | الوصف |
|--------|-------|
| `quranapp-main/` | أندرويد: مكتبة `quran-sdk` (1.1.0) مع اختباراتها، وتطبيق تجريبي (`SdkApiTestActivity`) |
| `ios/TaahudQuranSDK/` | iOS: حزمة Swift جديدة بديلة عن الملف المُجمَّع، بنفس واجهة أندرويد ونفس البيانات |
| `iOS-SDK/` | الملف المُجمَّع القديم (لا يعمل، انظر F1) وحزمة الموارد (`ayahinfo_1024.db` وغيرها) |
| `web-demo/` | نموذج ويب: صفحات مصحف المدينة، وتسجيل الورد، والبحث، والمفضلة، ومختبر `QuranApi` |
| `server/laravel-kit/` | حزمة Laravel لخادم تعاهد: مفتاح الإطلاق التدريجي، وإعدادات الصور، ومزامنة الورد والمفضلة |
| `tools/` | فحص البيانات وإصلاحها، وتوليد بيانات النموذج، وتجهيز حزمة الصور، وإعداد بيئة أندرويد |
| `docs/` | خطة الجاهزية والملاحظات والقرارات وتقارير الاختبار وأدلة الدمج |

## الوثائق
- [خطة الجاهزية وجدول الحالة](docs/readiness-plan.md)
- [الملاحظات الفنية](docs/findings.md) · [القرارات](docs/decisions.md) · [الأسئلة المفتوحة](docs/open-questions.md)
- [تقرير سلامة البيانات](docs/data-validation.md) · [تقرير اختبار أندرويد](docs/android-test-report.md)
- [تصميم iOS](docs/ios-design.md) · [دمج iOS](docs/integration-ios.md) · [دمج أندرويد](docs/integration-android.md)
- [مقترح واجهة Laravel للورد](docs/laravel-wird-api.md) · [قائمة التجربة والإطلاق](docs/pilot-checklist.md)
- [سجل التغييرات](CHANGELOG.md) · [إشعارات الأطراف الثالثة](NOTICE)

## أوامر سريعة

```bash
python3 tools/validate_data.py            # فحص البيانات (يكتب docs/data-validation.md)
python3 tools/build_web_data.py           # توليد بيانات النموذج من quran.json
bash tools/sync_ios_resources.sh          # نسخ البيانات لحزمة iOS
bash tools/package_pages.sh               # حزمة صور الصفحات + البصمات (dist/pages)
node web-demo/serve.js                    # النموذج محلياً على http://localhost:8765
```

```bash
# أندرويد (مرة واحدة: bash tools/setup_android_env.sh)
cd quranapp-main && bash gradlew :quran-sdk:testDebugUnitTest :quran-sdk:connectedDebugAndroidTest
```

```bash
# iOS (يعمل بدون Xcode)
cd ios/TaahudQuranSDK && ./run-tests.sh
```

## ملاحظات
- **البيانات موحّدة:** ترقيم الصفحات على **طبعة مصحف المدينة 1405**، وهي طبعة الصور المتوفرة (قرار D3).
- **مصدر الصور و `ayahinfo_1024.db`:** Quran.com (منشورة للعامة)، ويُذكر المصدر داخل التطبيق.
- **حدود الجزأين ٤ و١١:** تختلف بين المصادر، وتنتظر تأكيداً علمياً (F8).
