#!/bin/bash
# تثبيت أدوات بناء واختبار أندرويد بدون sudo: JDK 17 + Android SDK + محاكيين (API 24 و 35)
# الاستخدام: bash tools/setup_android_env.sh
set -euo pipefail

TOOLS="$HOME/tools"
SDK="$HOME/Library/Android/sdk"
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-mac-15641748_latest.zip"

mkdir -p "$TOOLS" "$SDK/cmdline-tools"

if [ ! -d "$TOOLS/jdk-17" ]; then
  echo "==> JDK 17"
  curl -fsSL -o "$TOOLS/jdk17.tar.gz" "$JDK_URL"
  mkdir -p "$TOOLS/jdk-17"
  tar -xzf "$TOOLS/jdk17.tar.gz" -C "$TOOLS/jdk-17" --strip-components=1
  rm "$TOOLS/jdk17.tar.gz"
fi
export JAVA_HOME="$TOOLS/jdk-17/Contents/Home"
"$JAVA_HOME/bin/java" -version

if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  echo "==> Android cmdline-tools"
  curl -fsSL -o "$TOOLS/cmdline.zip" "$CMDLINE_URL"
  unzip -q "$TOOLS/cmdline.zip" -d "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm "$TOOLS/cmdline.zip"
fi

SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
echo "==> SDK packages"
# yes ينتهي بـSIGPIPE عند إغلاق sdkmanager للإدخال، فنتجاهل حالة الأنبوب هنا
(set +o pipefail; yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses > /dev/null)
"$SDKMANAGER" --sdk_root="$SDK" \
  "platform-tools" "platforms;android-36" "build-tools;36.0.0" "emulator" \
  "system-images;android-35;google_apis;arm64-v8a" \
  "system-images;android-24;default;arm64-v8a"

AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
for spec in "api35:system-images;android-35;google_apis;arm64-v8a" "api24:system-images;android-24;default;arm64-v8a"; do
  name="${spec%%:*}"; img="${spec#*:}"
  if ! "$AVDMANAGER" list avd -c | grep -qx "$name"; then
    echo "no" | "$AVDMANAGER" create avd -n "$name" -k "$img" -d pixel_6 > /dev/null
  fi
done

echo "==> DONE"
echo "export JAVA_HOME=\"$JAVA_HOME\""
echo "export ANDROID_HOME=\"$SDK\""
