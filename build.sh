#!/bin/env bash

set -e

DO_CLEAN=false
DO_INSTALL=false
DO_DEBUG=false

for arg in "$@"; do
    case $arg in
        clean) DO_CLEAN=true ;;
        install) DO_INSTALL=true ;;
        debug) DO_DEBUG=true ;;
    esac
done

if [ "$DO_CLEAN" = true ]; then
    echo "Stopping Gradle Daemons..."
    ./gradlew --stop

    echo "Cleaning build artifacts and temporary files..."
    rm -rf .gradle build app/build app/release

    ./gradlew clean
fi

if [ "$DO_DEBUG" = true ]; then
    echo "Building Debug APK..."
    ./gradlew :app:assembleDebug
else
    echo "Building Release APK..."
    ./gradlew :app:assembleRelease
fi

APK_DIR="app/build/outputs/apk"
APK_FILE="$APK_DIR/debug/app-debug.apk"
SIGNED_APK_FILE="$APK_DIR/release/app-release-signed.apk"

echo "------------------------------------------------"
echo "Build complete!"

if [ "$DO_INSTALL" = true ]; then
    if [ "$DO_DEBUG" = true ]; then
        # Debug APK
        if [ -f "$APK_FILE" ]; then
            echo "Installing $APK_FILE to device..."
            adb install -r "$APK_FILE"
        else
            echo "Error: Debug APK not found at $APK_FILE"
            exit 1
        fi
    else
        # Signed Release APK
        if [ -f "$APK_FILE" ]; then
            cp "$APK_FILE" "$SIGNED_APK_FILE"
        fi

        if [ -f "$SIGNED_APK_FILE" ]; then
            echo "Installing $SIGNED_APK_FILE to device..."
            adb install -r "$SIGNED_APK_FILE"
        else
            echo "Error: Signed APK not found at $SIGNED_APK_FILE"
            exit 1
        fi
    fi
fi
