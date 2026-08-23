# Relief on a Mac

A Mac is sufficient for the practical Relief deployment path.

## What runs where

- Pixel 9a: official GrapheneOS base image, keeping verified boot, modem/IMS, emergency alerts and normal GrapheneOS updates.
- Relief: standalone Android app providing the app-selection setup flow and minimal HOME launcher.
- Mac: builds and sideloads the small Relief APK. It does not build the full Android OS.

A full custom GrapheneOS-derived ROM still requires an x86-64 Linux build host with substantially more RAM and storage. Relief V1 intentionally avoids that requirement.

## 1. Install GrapheneOS

Use the official GrapheneOS WebUSB installer from a supported Chromium-family browser on macOS. The official installer supports current macOS releases and the Pixel 9a. Follow the official instructions exactly, including relocking the bootloader after flashing.

https://grapheneos.org/install/web

This wipes the Pixel during initial installation.

## 2. Build Relief on Apple Silicon

Clone the repo and run:

```bash
git clone https://github.com/ryanbytes/Relief.git
cd Relief
chmod +x scripts/macos-build-and-install.sh
./scripts/macos-build-and-install.sh
```

The script installs/uses the Android command-line tools, Android platform tools and Gradle, installs Android SDK API 37 plus Build Tools 36.0.0, builds the debug-signed Relief APK and installs it over ADB when an authorized Pixel is connected.

Expected APK path:

```text
android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk
```

## 3. Make Relief the Home app

After installation, press Home and select **Relief** as the default Home application. The home screen exposes only:

- Phone
- Messages
- Maps
- Music
- Weather
- Relief Setup
- Settings

## 4. RCS

Open Relief Setup and complete the required RCS section:

1. Install sandboxed Google Play from GrapheneOS Apps.
2. Install Google Messages.
3. Set Google Messages as the default SMS application.
4. Give sandboxed Google Play services the Phone permission.
5. Enable ICC authentication for Play services if required by the carrier.
6. Open Google Messages and verify that RCS chats reports **Connected**.

Relief deliberately requires manual confirmation of the final RCS state because Google Messages does not provide a public API allowing another app to truthfully read its registration state.

## Full custom ROM later

The `scripts/build-relief.sh` path remains in the repository for producing a full `tegu-cur-user` GrapheneOS-derived image on a suitable x86-64 Linux host. It is not required for the Mac-only Relief V1 deployment.
