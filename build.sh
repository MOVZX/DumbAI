#!/bin/env bash

set -e

DO_CLEAN=false
DO_INSTALL=false

for arg in "$@"; do
    case $arg in
        clean) DO_CLEAN=true ;;
        install) DO_INSTALL=true ;;
    esac
done

if [ "$DO_CLEAN" = true ]; then
    echo "Stopping Gradle Daemons..."
    ./gradlew --stop
    echo "Cleaning build artifacts and temporary files..."
    rm -rf .gradle build app/build app/release
    ./gradlew clean
fi

echo "Building Release APK..."
./gradlew :app:assembleRelease

APK_DIR="app/build/outputs/apk"
SIGNED_SRC="$APK_DIR/release/app-release.apk"
SIGNED_DEST="$APK_DIR/release/app-release-signed.apk"

if [ -f "$SIGNED_SRC" ]; then
    cp "$SIGNED_SRC" "$SIGNED_DEST"
fi

echo "------------------------------------------------"
echo "Build complete!"

if [ "$DO_INSTALL" = true ]; then
    if [ -f "$SIGNED_DEST" ]; then
        echo "Installing $SIGNED_DEST to device..."
        adb install -r "$SIGNED_DEST"
    else
        echo "Error: Signed APK not found at $SIGNED_DEST"
        exit 1
    fi
fi
