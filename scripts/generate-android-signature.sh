#!/usr/bin/env bash
# Builds gradle/android-api-<level>.signature, the Java API of an Android version, which the Animal Sniffer check of
# kondor-core and kondor-outcome uses to fail the build when the code calls something that Android version lacks.
#
# The Java API comes from the android.jar of the Android SDK platform: from the Android SDK if the platform is installed
# ($ANDROID_HOME, $ANDROID_SDK_ROOT or the default SDK folder), otherwise from the platform zip in Google's SDK
# repository. That download is covered by the Android SDK License Agreement, as when sdkmanager installs it. Only the
# signature, the names of the classes and methods, is committed.
#
# It needs Java 11 or later (from $JAVA_HOME, or the java on the PATH) and the network, to download Animal Sniffer and
# ASM from Maven Central, and the platform when it is not installed.
#
# Usage: scripts/generate-android-signature.sh [api-level]   (default 33, Android 13)
#   ANDROID_JAR=/path/to/android.jar   use this jar instead
#   PLATFORM_ZIP=platform-34_r03.zip    the zip to download for a level other than 33

set -euo pipefail

API_LEVEL="${1:-33}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT="$ROOT/gradle/android-api-$API_LEVEL.signature"
MAVEN=https://repo1.maven.org/maven2
ANIMAL_SNIFFER=1.28
ASM=9.10.1

# the SHA-1 of the zip the committed android-api-33.signature was built from
PLATFORM_33_SHA1=eef99bd4617e80da85187a183eba356d787100d9

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javaOutput="$("$JAVA" -version 2>&1)" || { echo "Java 11 or later is needed: $JAVA does not run" >&2; exit 1; }
javaVersion="$(echo "$javaOutput" | head -1 | sed -E 's/.*version "(1\.)?([0-9]+).*/\2/')"
[ "$javaVersion" -ge 11 ] 2>/dev/null || { echo "Java 11 or later is needed, $JAVA is version $javaVersion" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

androidJar="${ANDROID_JAR:-}"
for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [ -z "$androidJar" ] && [ -n "$sdk" ] && [ -f "$sdk/platforms/android-$API_LEVEL/android.jar" ]; then
        androidJar="$sdk/platforms/android-$API_LEVEL/android.jar"
    fi
done
if [ -z "$androidJar" ]; then
    zip="${PLATFORM_ZIP:-}"
    if [ -z "$zip" ]; then
        [ "$API_LEVEL" = "33" ] || { echo "Set PLATFORM_ZIP for API level $API_LEVEL (see https://dl.google.com/android/repository/repository2-3.xml)" >&2; exit 1; }
        zip=platform-33_r02.zip
    fi
    echo "Downloading $zip (Android SDK License Agreement applies)"
    curl -fsS -o "$WORK/platform.zip" "https://dl.google.com/android/repository/$zip"
    if [ "$zip" = "platform-33_r02.zip" ]; then
        sha1="$(shasum -a 1 "$WORK/platform.zip" | cut -d' ' -f1)"
        [ "$sha1" = "$PLATFORM_33_SHA1" ] || { echo "Unexpected SHA-1 $sha1 for $zip" >&2; exit 1; }
    fi
    unzip -q -j "$WORK/platform.zip" "*/android.jar" -d "$WORK"
    androidJar="$WORK/android.jar"
fi
echo "Using $androidJar"

curl -fsS -o "$WORK/animal-sniffer.jar" "$MAVEN/org/codehaus/mojo/animal-sniffer/$ANIMAL_SNIFFER/animal-sniffer-$ANIMAL_SNIFFER.jar"
curl -fsS -o "$WORK/asm.jar" "$MAVEN/org/ow2/asm/asm/$ASM/asm-$ASM.jar"

"$JAVA" -cp "$WORK/animal-sniffer.jar:$WORK/asm.jar" "$ROOT/scripts/BuildAndroidSignature.java" "$androidJar" "$OUTPUT"
echo "Written $OUTPUT"
[ "$API_LEVEL" = "33" ] || echo "Point the signature dependency of kondor-core and kondor-outcome to android-api-$API_LEVEL.signature"
