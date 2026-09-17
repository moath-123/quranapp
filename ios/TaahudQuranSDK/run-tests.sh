#!/bin/bash
# Runs the core tests with Command Line Tools only (no Xcode): Swift Testing lives outside the default search path.
set -euo pipefail
cd "$(dirname "$0")"
F=/Library/Developer/CommandLineTools/Library/Developer/Frameworks
L=/Library/Developer/CommandLineTools/Library/Developer/usr/lib
if xcode-select -p | grep -q "Xcode.app"; then
  exec swift test "$@"
fi
exec swift test -Xswiftc -F -Xswiftc "$F" -Xlinker -F -Xlinker "$F" \
  -Xlinker -rpath -Xlinker "$F" -Xlinker -rpath -Xlinker "$L" "$@"
