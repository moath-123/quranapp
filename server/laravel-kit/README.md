# حزمة Laravel للمصحف (تعاهد)

ما تضيفه لخادم تعاهد:

| المسار | الوظيفة |
|---|---|
| `GET /api/quran/app-config` | مفتاح `new_mushaf` (إطلاق تدريجي) + إعدادات صور الصفحات. متاح للزوار (بـ`X-Device-Id`) وللمسجلين |
| `GET/POST /api/quran/wirds` | مزامنة الورد بين أجهزة المستخدم (upsert بـ`client_id`، والحذف يُزامَن أيضاً) |
| `GET /api/quran/wirds/stats?tz=` | آيات اليوم، آخر 7 أيام، الأيام المتتالية، الآية التالية |
| `GET/PUT /api/quran/bookmarks` | المفضلة (`add`/`remove`) |
| `GET/PUT /api/quran/position` | آخر موضع قراءة |

**الآيات تُحفظ برقمها في المصحف (1–6236)**، لا برقم الصفحة، فتبقى البيانات صحيحة مهما تغيّرت طبعة العرض.

## التثبيت
المتطلبات: Laravel 11 فما فوق + Sanctum (`php artisan install:api`).

```bash
bash server/laravel-kit/install.sh /path/to/taahud-backend
cd /path/to/taahud-backend
php artisan migrate
php artisan test --filter=Quran
```
السكربت ينسخ الملفات، ويضيف `require __DIR__.'/quran.php';` إلى `routes/api.php`، ويضيف `HasApiTokens` لنموذج `User` إن لم يكن موجوداً. تكراره آمن.

## الإعدادات (`.env`)
```dotenv
# المصحف الجديد — الافتراضي مغلق
QURAN_NEW_MUSHAF_ENABLED=false     # مفتاح الإيقاف الكامل: false = الجميع على المصحف الحالي فوراً
QURAN_NEW_MUSHAF_ROLLOUT=0         # نسبة المستخدمين 0–100 (ثابتة لكل مستخدم)
QURAN_NEW_MUSHAF_ALLOWLIST=12,57   # معرّفات المختبرين الداخليين (يحصلون عليه دائماً)

# صور الصفحات — من dist/pages/pages-manifest.json (tools/package_pages.sh)
QURAN_PAGES_URL=https://cdn.taahud.example/quran/images_1024-v1.zip
QURAN_PAGES_SHA256=882e5c7bc8d43a4efac679e7c85b64e1fc059783dab6babc02365f2c36a286cb
QURAN_PAGES_VERSION=1
```
بعد أي تعديل: `php artisan config:clear` (أو `config:cache` في الإنتاج). التطبيق يقرأ الإعدادات كل 5 دقائق على الأكثر (`Cache-Control: max-age=300`).

## خطوات الإطلاق (docs/pilot-checklist.md)
| المرحلة | الإعداد |
|---|---|
| داخلي | `ENABLED=true`، `ROLLOUT=0`، `ALLOWLIST=<معرّفات الفريق>` |
| ١ | `ROLLOUT=5` |
| ٢ | `ROLLOUT=25` |
| ٣ | `ROLLOUT=100` |
| **رجوع فوري** | `ENABLED=false` ثم `php artisan config:cache` |

رفع النسبة يُبقي كل من حصل على المصحف الجديد سابقاً (التوزيع ثابت لكل مستخدم)، ويختبر ذلك `AppConfigTest::test_rollout_is_stable_and_monotonic`.

## الاختبارات
15 اختباراً في `tests/Feature/Quran`، وتشتغل في GitHub Actions على مشروع Laravel جديد في كل تعديل (`.github/workflows/laravel-kit.yml`).
