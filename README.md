# Relief

Relief is a deliberately minimal Android experience for the Google Pixel 9a (`tegu`): calls, SMS/MMS/RCS, messaging services, navigation, music, severe-weather alerts, and location sharing.

## Recommended V1: Mac + official GrapheneOS

You do **not** need a Linux workstation for the practical Relief deployment.

Use official GrapheneOS on the Pixel 9a as the signed, updateable base OS, then install the standalone `ReliefSetup` APK from a Mac. Relief provides:

- required Google Messages / RCS setup checks
- selectable messaging, navigation, music, weather and location-sharing apps
- a minimal HOME launcher exposing Phone, Messages, Maps, Music, Weather, Setup and Settings

See [`docs/MAC.md`](docs/MAC.md).

On an Apple Silicon Mac:

```bash
git clone https://github.com/ryanbytes/Relief.git
cd Relief
chmod +x scripts/macos-build-and-install.sh
./scripts/macos-build-and-install.sh
```

The standalone APK uses public Android APIs and can be built normally on macOS. The script installs/uses Android SDK API 37, Build Tools 36.0.0, Gradle and ADB, then builds and installs the APK when an authorized Pixel is attached.

Expected output:

```text
android/ReliefSetup/build/outputs/apk/debug/ReliefSetup-debug.apk
```

## RCS is mandatory

Relief treats RCS as required. The setup flow checks that:

- sandboxed Google Play is installed
- Google Messages is installed
- Google Messages is the default SMS app
- Play services has Phone permission
- the user has explicitly verified Google Messages reports RCS **Connected**

Some carriers also require the GrapheneOS ICC-authentication permission for Play services. Google does not expose a public API for third-party apps to read Google Messages' actual RCS registration state, so Relief does not fabricate that status.

## Selectable apps

- Messaging: Signal, WhatsApp, Telegram, Messenger
- Navigation: Google Maps, Waze, Organic Maps, HERE WeGo
- Music: Amazon Music, Spotify, YouTube Music, Pandora, VLC
- Weather: MyRadar, Weather & Radar, The Weather Channel
- Location sharing: Google Maps, OwnTracks, or none

## What Relief does not remove

The GrapheneOS base retains the infrastructure that should not be stripped merely to save a small amount of space:

- cellular modem / IMS / VoLTE / VoWiFi
- Cell Broadcast / emergency alerts
- Bluetooth and Wi-Fi framework support
- GNSS and network-location plumbing
- PackageInstaller / permission controller
- WebView
- sandboxed Google Play compatibility layer
- Android Verified Boot

## Full custom ROM path

The repository also contains the experimental full-ROM path pinned to GrapheneOS `2026081300` (Android 17):

- device: Pixel 9a
- codename: `tegu`
- build target: `tegu-cur-user`

A complete GrapheneOS-derived build requires an x86-64 Linux host with at least 32 GiB RAM and roughly 200+ GiB of working storage. GitHub Actions is intentionally not used.

On a suitable Linux builder:

```bash
sudo ./scripts/install-build-deps-debian.sh
./scripts/check-host.sh
./scripts/build-relief.sh
```

For a signed, bootloader-lockable production release, generate and protect unique signing keys first and follow `docs/BUILD.md`. Never commit private signing keys.

## License

Relief-specific code is MIT licensed. GrapheneOS/AOSP components retain their upstream licenses.
