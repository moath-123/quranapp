#!/bin/bash
# نسخ مصادر البيانات الموحّدة إلى حزمة iOS (CI يتحقق أنها مطابقة).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/ios/TaahudQuranSDK/Sources/TaahudQuranCore/Resources"
mkdir -p "$DEST"
cp "$ROOT/quranapp-main/quran-sdk/src/main/assets/quran.json" "$DEST/quran.json"
cp "$ROOT/iOS-SDK/QuranSDK_QuranSDK.bundle/ayahinfo_1024.db" "$DEST/ayahinfo_1024.db"
echo "synced to $DEST"
