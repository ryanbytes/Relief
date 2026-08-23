# Relief

Relief is a deliberately minimal Android launcher for people who want a phone to behave like a phone: calls, SMS/MMS/RCS, messaging services, navigation, music, severe-weather alerts, and location sharing.

The launcher is vendor-neutral. It is designed to run on Android 6.0+ (`minSdk 23`) on Pixels, Samsung, Motorola, OnePlus and other Android devices without replacing the operating system.

## What Relief does

Relief provides:

- a minimal HOME screen with Phone, Messages, Maps, Music, Weather and Apps
- required Google Messages / RCS setup checks
- selectable messaging, navigation, music, weather and location-sharing apps
- persistent primary-app choices rather than guessing which installed app to launch
- a small selected-apps screen instead of a conventional all-apps drawer
- direct access to Android Settings when needed

Relief does **not** disable Android security, telephony, emergency alerts, Bluetooth, location services, verified boot, or normal OS updates.

## RCS

Relief treats RCS as required and uses Google Messages as the RCS client.

The setup flow checks that:

- Google Play services is installed
- Google Messages is installed
- Google Messages is the default SMS app
- the user has explicitly verified Google Messages reports RCS **Connected**

On GrapheneOS, Relief additionally detects the GrapheneOS Apps environment and checks the Play services Phone permission needed by the sandboxed-Google RCS path.

Actual RCS registration depends on Google Messages, the carrier and the device. Android does not expose a public API that lets Relief reliably read Google Messages' RCS registration state, so Relief deliberately requires the user to confirm the Connected state instead of inventing one.

## Selectable apps

- Messaging: Signal, WhatsApp, Telegram, Messenger
- Navigation: Google Maps, Waze, Organic Maps, HERE WeGo
- Music: Amazon Music, Spotify, YouTube Music, Pandora, VLC
- Weather: MyRadar, Weather & Radar, The Weather Channel
- Location sharing: Google Maps, OwnTracks, or none

## Downloadable build

The repository's `Build Relief Launcher` workflow lints, compiles, verifies the APK signature, verifies the package name and HOME intent, then publishes a rolling test build as `relief-latest` when `main` passes.

The rolling APK is intentionally debug-signed for evaluation. A stable public release should use a persistent private signing key stored outside the repository.

## Build on a Mac

On Apple Silicon or Intel macOS:

```bash
git clone https://github.com/ryanbytes/Relief.git
cd Relief
chmod +x scripts/macos-build-and-install.sh
./scripts/macos-build-and-install.sh
```

The script installs/uses Java 17, Gradle, Android SDK API 36, Build Tools 36.0.0 and ADB. It runs Android lint, builds the APK, verifies its signature and HOME intent, then installs it if an authorized Android phone is connected.

Expected output:

```text
android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk
```

After installation, press Home and select **Relief** as the Home app.

## Experimental full-ROM work

Older experimental files for a GrapheneOS-derived Pixel 9a build remain in the repository for reference. They are not the recommended product direction. The launcher approach is smaller, safer, works across Android vendors, preserves OEM/GrapheneOS updates, and can be built on a Mac.

## License

Relief-specific code is MIT licensed. Third-party apps and Android components retain their own licenses.
