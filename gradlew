#!/bin/env bash


export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"

GRADLE_BIN=$(which gradle)

if [ -z "$GRADLE_BIN" ]; then
    echo "Error: 'gradle' not found in PATH."
    exit 1
fi

JDK21="/multimedia/Android/jdk21/linux-x86"

if [ -d "$JDK21" ]; then
    export JAVA_HOME="$JDK21"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Build Environment:"
echo "- Java: $(java -version 2>&1 | head -n 1)"
echo "- SDK: $ANDROID_HOME"

"$GRADLE_BIN" -Pandroid.builder.sdkDownload=false "$@"
