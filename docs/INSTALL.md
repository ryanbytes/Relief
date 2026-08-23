# Installing Relief on Pixel 9a

This guide is for a **signed `release` build** generated from this repository. A `dev` build is different: keep the bootloader unlocked and do not treat it as a secure daily-driver OS.

## Before touching the phone

- Confirm the device is a Google Pixel 9a (`tegu`).
- Back up anything you care about. Unlocking and later locking the bootloader both wipe user data.
- Use recent Android Platform Tools / `fastboot`.
- Keep the Relief release signing keys backed up. Never place private keys on the phone.
- Keep the factory image ZIP and its signature together.

## 1. Enable OEM unlocking

On the currently installed OS, enable Developer options and OEM unlocking, then reboot to the bootloader.

Check that fastboot sees the phone:

```bash
fastboot devices
fastboot getvar product
```

`product` must report `tegu`. Stop if it does not.

## 2. Unlock the bootloader

```bash
fastboot flashing unlock
```

Confirm the operation on the phone. This wipes the device.

## 3. Flash the signed Relief factory image

Extract the generated `tegu-install-BUILD_NUMBER.zip`, enter its directory, and run its unmodified factory script:

```bash
tar xvf tegu-install-BUILD_NUMBER.zip
cd tegu-install-BUILD_NUMBER
bash flash-all.sh
```

Do not hand-edit `flash-all.sh`.

The current GrapheneOS factory-image generator puts the release's `avb_pkmd.bin` in the factory image and the generated script performs:

```text
fastboot erase avb_custom_key
fastboot flash avb_custom_key avb_pkmd.bin
```

before flashing the OS images. That establishes the public key your Pixel will use for Android Verified Boot for this Relief signing identity.

Wait until the script finishes. Do not unplug or interact with the phone midway through the flash.

## 4. Lock the bootloader for a signed release

Only do this for a release built with your persistent private Relief signing keys, **never** for the `dev` build using public test keys.

With the phone back in the bootloader:

```bash
fastboot flashing lock
```

Confirm on the phone. This wipes the device again.

If anything about the signed build or AVB key provisioning is uncertain, leave the bootloader unlocked until the image has been checked. Locking around an incorrectly signed image can make the phone fail to boot.

## 5. Relief first boot

GrapheneOS's normal setup runs first for security, network, lock-screen and device basics. Pressing its final Start button hands off to `ReliefSetup`.

### RCS is mandatory

Complete this section before finishing Relief Setup:

1. Open GrapheneOS Apps from the Relief setup screen.
2. Install sandboxed Google Play components.
3. Grant Google Play services the **Phone** permission.
4. If your carrier uses GSMA TS.43 verification, enable **Settings → Apps → Sandboxed Google Play → Play services special permissions → ICC authentication with device identifiers**.
5. Install Google Messages from Google Play.
6. Make Google Messages the default SMS app.
7. Open Google Messages → Settings → RCS chats and wait for **Connected**.
8. Return to Relief Setup and check the RCS confirmation box.

Relief verifies the parts Android exposes—Play/Play Services presence, Google Messages presence/default-SMS role, and the Play services Phone permission. Google Messages does not expose a public third-party API for reading its live RCS registration state, so the final `Connected` check is intentionally manual rather than fake.

GrapheneOS currently documents Google Messages RCS as known to work in the Owner user profile with sandboxed Google Play, with Phone permission and optional ICC authentication depending on carrier.

Upstream reference: https://grapheneos.org/usage#rcs

## 6. Choose the rest of the phone

Relief Setup then offers the deliberately small app set:

- Messaging: Signal, WhatsApp, Telegram, Messenger
- Navigation: Google Maps, Waze, Organic Maps, HERE WeGo
- Music: Amazon Music, Spotify, YouTube Music, Pandora, VLC/local music
- Weather: MyRadar, Weather & Radar, The Weather Channel
- Location sharing: Google Maps, OwnTracks, none

It builds an install queue and opens the Play Store for missing selections one at a time. Apps are not silently downloaded from random APK mirrors.

## 7. Functional acceptance test

Do not call a build a daily driver until all of these have actually been tested on the carrier/SIM you use:

- incoming call
- outgoing call
- emergency-dialer screen is present (do **not** place a test 911 call without coordinating with local authorities)
- SMS send/receive
- MMS send/receive with image attachment
- RCS send/receive and group chat
- Wi-Fi calling if you rely on it
- VoLTE/5G voice
- Bluetooth call audio
- Bluetooth music audio
- navigation GPS lock and turn-by-turn voice
- location sharing
- severe-weather notifications
- alarm after overnight idle
- reboot and unlock
- app install/update through GrapheneOS Apps and Google Play

Until those tests pass, the build is a test ROM, not a reliable phone.

## Development build warning

For `RELIEF_BUILD_MODE=dev`, raw images use Android public test keys. Keep the bootloader unlocked. This is useful for iteration but deliberately not secure against OS replacement.

## Returning to stock Android

Before restoring the Google stock OS and locking its bootloader, remove the custom AVB trust key while the bootloader is unlocked:

```bash
fastboot erase avb_custom_key
```

Then flash current official Google factory images and only lock after the stock image is correctly installed.

GrapheneOS documents the same custom-key cleanup step when returning its Pixels to stock.