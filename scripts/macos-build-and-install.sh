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

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager is not on PATH after installing android-commandlinetools." >&2
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not on PATH after installation." >&2
  exit 1
fi
if ! command -v adb >/dev/null 2>&1; then
  echo "ADB is not on PATH after installing android-platform-tools." >&2
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

echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Gradle: $(gradle --version | awk '/Gradle / {print $2; exit}')"

echo "Building ReliefSetup debug APK..."
gradle --no-daemon :android:ReliefSetup:assembleDebug

APK="$ROOT/android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Build completed but APK was not found at $APK" >&2
  exit 1
fi

echo
printf 'APK: %s\n' "$APK"

if adb devices | awk 'NR>1 && $2=="device" {found=1} END{exit !found}'; then
  echo "Pixel detected. Installing ReliefSetup..."
  adb install -r "$APK"
  echo "Installed. Press Home and choose Relief as the Home app, then open Relief Setup."
else
  echo "No authorized Android device detected."
  echo "Connect the Pixel with USB debugging enabled, authorize the Mac, then run:"
  echo "  adb install -r '$APK'"
fi
