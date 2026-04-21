#!/bin/env bash


echo "Cleaning build artifacts..."
cd "$(dirname "$0")"
./gradlew clean

set -e

cd "$(dirname "$0")"

# Use environment variable for SDK if available, fallback to /opt/android-sdk
SDK_PATH="${ANDROID_HOME:-/opt/android-sdk}"
SIGNER="$SDK_PATH/build-tools/34.0.0/apksigner"

if [ ! -f "$SIGNER" ]; then
    # Try to find any apksigner if the specific version isn't there
    SIGNER=$(find "$SDK_PATH/build-tools" -name apksigner | head -n 1)
fi

echo "Building Release APK..."
./gradlew assembleRelease

echo "Signing APK..."
if [ -f "$SIGNER" ]; then
    "$SIGNER" sign --ks dumbai.jks --ks-pass pass:dumbai123 --key-pass pass:dumbai123 --out app/build/outputs/apk/release/app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk
else
    echo "Warning: apksigner not found. APK build succeeded but was not signed."
fi

echo "------------------------------------------------"
echo "Build complete!"
ls -l app/build/outputs/apk/release/app-release-signed.apk 2>/dev/null || echo "Output APK not found."
cd "$(dirname "$0")"

APK="app/build/outputs/apk/release/app-release-signed.apk"

if [ ! -f "$APK" ]; then
    echo "Error: Signed APK not found. Run ./build.sh first."
    exit 1
fi

echo "Installing $APK to device..."
adb install -r "$APK"
