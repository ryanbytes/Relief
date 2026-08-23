# Relief

Relief is a minimal Android build for the Google Pixel 9a (`tegu`) based on GrapheneOS. Its purpose is deliberately narrow: calls, SMS/MMS/RCS, messaging services, navigation, music, severe-weather alerts, and location sharing.

## Status

Early buildable source tree. The project pins GrapheneOS `2026081300` (Android 17) for the first reproducible build.

Relief does **not** weaken the modem, IMS, emergency-alert, location, WebView, permission, verified-boot, or sandboxed-Google compatibility infrastructure. It removes only application-layer components that are safe to omit, and adds `ReliefSetup` for first-boot app selection.

## Pixel 9a

- Device: Pixel 9a
- Codename: `tegu`
- Build target: `tegu-cur-user`
- Base: GrapheneOS Android 17
- Initial base tag: `2026081300`
- RCS path: Google Messages + GrapheneOS sandboxed Google Play compatibility layer

## First boot

ReliefSetup treats Google Messages/RCS as required and presents optional selections for:

- Messaging: Signal, WhatsApp, Telegram, Messenger
- Navigation: Google Maps, Waze, Organic Maps, HERE WeGo
- Music: Amazon Music, Spotify, YouTube Music, Pandora, VLC
- Weather: Breezy Weather, MyRadar, The Weather Channel
- Location sharing: Google Maps, OwnTracks, or none

The setup app can verify that Google Messages is installed and is the default SMS handler. Google does not expose a public Android API for third-party apps to read Google Messages' actual RCS registration state, so ReliefSetup does not fabricate an RCS `Connected` result; it opens Messages so the user can confirm RCS registration.

## Building

A complete GrapheneOS build currently requires an x86-64 Linux host with at least 32 GiB RAM and roughly 200+ GiB of working storage. GitHub Actions is intentionally not used.

On a suitable Debian/Ubuntu Linux builder:

```bash
git clone https://github.com/ryanbytes/Relief.git
cd Relief
sudo ./scripts/install-build-deps-debian.sh
./scripts/check-host.sh
./scripts/build-relief.sh
```

`build-relief.sh` syncs the pinned GrapheneOS source, generates Pixel vendor files, injects ReliefSetup and the Relief product fragment, then builds `tegu-cur-user`.

For a signed, bootloader-lockable production release, generate and protect unique signing keys first and run the release step described in `docs/BUILD.md`. Never ship or commit private signing keys.

## Safety rules

Relief deliberately keeps:

- cellular modem / IMS / VoLTE / VoWiFi support
- Cell Broadcast / emergency alerts
- Bluetooth and Wi-Fi framework support
- GNSS and network-location plumbing
- PackageInstaller / permission controller
- WebView
- GrapheneOS sandboxed Google Play compatibility layer
- Android Verified Boot support

Stripping these saves little and creates failure modes in calling, emergency alerts, RCS, maps, Bluetooth audio, file pickers, or app compatibility.

## License

Relief-specific code is MIT licensed. GrapheneOS/AOSP components retain their upstream licenses.