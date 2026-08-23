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

need_brew=0
for cmd in gradle sdkmanager adb; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    need_brew=1
  fi
done

if [[ "$need_brew" == 1 ]]; then
  if ! command -v brew >/dev/null 2>&1; then
    echo "Homebrew is required. Install it from https://brew.sh and rerun this script." >&2
    exit 1
  fi
  echo "Installing Android command-line tools, platform-tools and Gradle..."
  brew install gradle || true
  brew install --cask android-commandlinetools || true
  brew install --cask android-platform-tools || true
fi

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager is still not on PATH. If Android Studio is installed, add its cmdline-tools/latest/bin directory to PATH." >&2
  exit 1
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
mkdir -p "$ANDROID_HOME"

echo "Installing Android SDK components..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platforms;android-37" \
  "build-tools;36.0.0" \
  "platform-tools"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not found after setup." >&2
  exit 1
fi

echo "Building ReliefSetup debug APK..."
gradle --no-daemon :android:ReliefSetup:assembleDebug

APK="$ROOT/android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Build completed but APK was not found at $APK" >&2
  exit 1
fi

echo
printf 'APK: %s\n' "$APK"

if command -v adb >/dev/null 2>&1 && adb devices | awk 'NR>1 && $2=="device" {found=1} END{exit !found}'; then
  echo "Pixel detected. Installing ReliefSetup..."
  adb install -r "$APK"
  echo "Installed. Open Relief Setup on the Pixel."
else
  echo "No authorized Android device detected."
  echo "Connect the Pixel with USB debugging enabled, authorize the Mac, then run:"
  echo "  adb install -r '$APK'"
fi
