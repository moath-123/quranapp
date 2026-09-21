#!/bin/bash
# Runs the core tests with Xcode, or with Command Line Tools only (Swift Testing then lives outside
# the default search path).
#
# Builds go to ~/Library/Caches: when the repo sits in an iCloud-synced folder (Desktop/Documents),
# macOS re-adds extended attributes to build products and ad-hoc code signing of the test bundle fails.
set -euo pipefail
cd "$(dirname "$0")"
SCRATCH="${SCRATCH_PATH:-$HOME/Library/Caches/TaahudQuranSDK-build}"
if xcode-select -p | grep -q "Xcode.app"; then
  exec swift test --scratch-path "$SCRATCH" "$@"
fi
F=/Library/Developer/CommandLineTools/Library/Developer/Frameworks
L=/Library/Developer/CommandLineTools/Library/Developer/usr/lib
exec swift test --scratch-path "$SCRATCH" -Xswiftc -F -Xswiftc "$F" -Xlinker -F -Xlinker "$F" \
  -Xlinker -rpath -Xlinker "$F" -Xlinker -rpath -Xlinker "$L" "$@"
