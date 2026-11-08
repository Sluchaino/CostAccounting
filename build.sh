#!/bin/sh
set -eu
cd "$(dirname "$0")"
sh ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/CostAccounting-Android.apk
echo 'Ready: dist/CostAccounting-Android.apk'
