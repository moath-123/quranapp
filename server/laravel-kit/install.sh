#!/bin/bash
# تثبيت حزمة المصحف في مشروع Laravel (11 فما فوق) مع Sanctum.
# الاستخدام: bash server/laravel-kit/install.sh /path/to/laravel-app
# آمن للتكرار: لا يكرر السطور المضافة، ويستبدل ملفات الحزمة فقط.
set -euo pipefail

KIT="$(cd "$(dirname "$0")" && pwd)"
APP="${1:?usage: install.sh /path/to/laravel-app}"
cd "$APP"
[ -f artisan ] || { echo "not a Laravel app: $APP" >&2; exit 1; }

copy() { mkdir -p "$(dirname "$2")"; cp "$KIT/$1" "$2"; echo "  + $2"; }

echo "==> files"
copy config/quran.php config/quran.php
copy routes/quran.php routes/quran.php
copy app/Support/Quran/Rollout.php app/Support/Quran/Rollout.php
for f in WirdEntry AyahBookmark ReadingPosition; do copy "app/Models/$f.php" "app/Models/$f.php"; done
for f in AppConfigController WirdController BookmarkController ReadingPositionController; do
  copy "app/Http/Controllers/Quran/$f.php" "app/Http/Controllers/Quran/$f.php"
done
copy app/Http/Requests/Quran/SyncWirdEntriesRequest.php app/Http/Requests/Quran/SyncWirdEntriesRequest.php
copy database/migrations/2026_09_21_000001_create_quran_tables.php database/migrations/2026_09_21_000001_create_quran_tables.php
for f in AppConfigTest WirdSyncTest BookmarksAndPositionTest; do
  copy "tests/Feature/Quran/$f.php" "tests/Feature/Quran/$f.php"
done

echo "==> routes/api.php"
[ -f routes/api.php ] || { echo "routes/api.php missing — run: php artisan install:api" >&2; exit 1; }
grep -q "quran.php" routes/api.php || printf "\nrequire __DIR__.'/quran.php';\n" >> routes/api.php

echo "==> User model (Sanctum HasApiTokens)"
if ! grep -q "HasApiTokens" app/Models/User.php; then
  python3 - app/Models/User.php <<'PY'
import re, sys
p = sys.argv[1]; s = open(p).read()
s = s.replace("namespace App\\Models;\n", "namespace App\\Models;\n\nuse Laravel\\Sanctum\\HasApiTokens;\n", 1)
s = re.sub(r"(class User extends [^{]+\{\s*(?:/\*\*.*?\*/\s*)?)use ", r"\1use HasApiTokens, ", s, count=1, flags=re.S)
open(p, "w").write(s)
PY
fi
grep -q "HasApiTokens," app/Models/User.php || { echo "add 'use HasApiTokens' to App\\Models\\User manually" >&2; exit 1; }

echo "==> done. Next: php artisan migrate && php artisan test --filter=Quran"
