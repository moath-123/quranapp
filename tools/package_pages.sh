#!/bin/bash
# تجهيز حزمة صور الصفحات لاستضافتها على خادم تعاهد (قرار D2).
#
# الناتج في dist/pages/:
#   images_1024-v<N>.zip   — الصفحات داخل width_1024/ (نفس شكل ملف Quran.com الذي يتوقعه PagesInstaller)
#   SHA256SUMS             — بصمة الملف المضغوط + بصمة كل صفحة
#   pages-manifest.json    — ما يُكتب في إعدادات التطبيق (الرابط، البصمة، النسخة، الحجم)
#
# الاستخدام:
#   bash tools/package_pages.sh [مجلد_الصور] [النسخة] [رابط_الاستضافة_الأساسي]
#   bash tools/package_pages.sh web-demo/pages 1 https://cdn.taahud.example/quran
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$ROOT/web-demo/pages}"
VERSION="${2:-1}"
BASE_URL="${3:-https://CHANGE-ME.example/quran}"
OUT="$ROOT/dist/pages"
NAME="images_1024-v${VERSION}.zip"

missing=0
for i in $(seq 1 604); do
  f="$SRC/$(printf 'page%03d.png' "$i")"
  if [ ! -f "$f" ]; then echo "missing: $f" >&2; missing=$((missing + 1)); fi
done
[ "$missing" -eq 0 ] || { echo "$missing pages missing — aborting" >&2; exit 1; }

rm -rf "$OUT" && mkdir -p "$OUT/stage/width_1024"
cp "$SRC"/page[0-9][0-9][0-9].png "$OUT/stage/width_1024/"
# تاريخ ثابت وترتيب ثابت ليكون الملف الناتج متطابقاً في كل مرة
find "$OUT/stage" -exec touch -t 202001010000 {} +
(cd "$OUT/stage" && find width_1024 | LC_ALL=C sort | zip -X -q -9 "../$NAME" -@)
rm -rf "$OUT/stage"

ZIP_SHA=$(shasum -a 256 "$OUT/$NAME" | cut -d' ' -f1)
ZIP_SIZE=$(stat -f%z "$OUT/$NAME" 2>/dev/null || stat -c%s "$OUT/$NAME")
{
  echo "$ZIP_SHA  $NAME"
  (cd "$SRC" && shasum -a 256 page[0-9][0-9][0-9].png | sed 's#  #  width_1024/#')
} > "$OUT/SHA256SUMS"

cat > "$OUT/pages-manifest.json" <<JSON
{
  "edition": "madani-1405",
  "width": 1024,
  "pages": 604,
  "version": $VERSION,
  "url": "$BASE_URL/$NAME",
  "sha256": "$ZIP_SHA",
  "bytes": $ZIP_SIZE
}
JSON

echo "archive : $OUT/$NAME ($ZIP_SIZE bytes)"
echo "sha256  : $ZIP_SHA"
echo "manifest: $OUT/pages-manifest.json"
