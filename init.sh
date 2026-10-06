#!/usr/bin/env bash
set -euo pipefail

echo "Repository: $(pwd)"
echo "Harness status: bootstrapped"
echo ""
echo "Base verification: running assembleDebug + testDebugUnitTest..."
./gradlew assembleDebug testDebugUnitTest --console=plain
echo ""
echo "Base verification passed."
echo "Manual follow-up (con dispositivo conectado):"
echo "  - Windows: gradlew.bat :app:installDebug"
echo "  - Unix:    ./gradlew :app:installDebug"
echo "No long-running processes started."
exit 0
