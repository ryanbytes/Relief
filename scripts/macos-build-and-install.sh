#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This script is for macOS." >&2
  exit 1
fi

if [[ "$(uname -m)" != "arm64" && "$(uname -m)" != "x86_64" ]]; then
  echo "Unsupported Mac architecture: $(uname -m)" >&2
  exit 1
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew is required. Install it from https://brew.sh and rerun this script." >&2
  exit 1
fi

echo "Ensuring Android build tools are installed..."
brew install openjdk@17 || true
brew install gradle || true
brew install --cask android-commandlinetools || true
brew install --cask android-platform-tools || true

export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
mkdir -p "$ANDROID_HOME"

for command in sdkmanager gradle adb; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "$command is not available after tool installation." >&2
    exit 1
  fi
done

echo "Installing stable Android SDK components..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "platform-tools"

echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Gradle: $(gradle --version | awk '/Gradle / {print $2; exit}')"

echo "Linting and building Relief Launcher..."
gradle --no-daemon :android:ReliefSetup:lintDebug :android:ReliefSetup:assembleDebug

APK="$ROOT/android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk"
APKSIGNER="$ANDROID_HOME/build-tools/36.0.0/apksigner"
AAPT="$ANDROID_HOME/build-tools/36.0.0/aapt"

if [[ ! -s "$APK" ]]; then
  echo "Build completed but APK was not found or was empty: $APK" >&2
  exit 1
fi

"$APKSIGNER" verify --verbose --print-certs "$APK"
BADGING="$($AAPT dump badging "$APK")"
printf '%s\n' "$BADGING" | grep -q "package: name='app.relief.setup'"
MANIFEST="$($AAPT dump xmltree "$APK" AndroidManifest.xml)"
printf '%s\n' "$MANIFEST" | grep -q "android.intent.category.HOME"

printf '\nVerified APK: %s\n' "$APK"

if adb devices | awk 'NR>1 && $2=="device" {found=1} END{exit !found}'; then
  echo "Android device detected. Installing Relief Launcher..."
  adb install -r "$APK"
  echo "Installed. Press Home and choose Relief as the Home app, then open Relief Setup."
else
  echo "No authorized Android device detected."
  echo "Connect a phone with USB debugging enabled, authorize the Mac, then run:"
  echo "  adb install -r '$APK'"
fi
