# Building Relief for Pixel 9a

Relief is pinned to the GrapheneOS `2026081300` stable source tag for its first hardware build. The target is Google Pixel 9a (`tegu`) and the production variant is `tegu-cur-user`.

## Hard host requirements

GrapheneOS currently documents:

- x86-64 Linux
- 32 GiB RAM minimum
- 136 GiB+ source storage for a normal sync, or about 90 GiB+ for a lightweight sync
- another 100 GiB+ for a typical full multiarch OS build
- Node.js 24 LTS and yarn for `adevtool`

Relief's host check defaults to **220 GiB free**. More is preferable; 300 GiB avoids pointless storage pressure during repeat builds.

Official upstream reference: https://grapheneos.org/build

## Supported build host

Debian 12 is a good boring choice.

```bash
git clone https://github.com/ryanbytes/Relief.git
cd Relief
sudo bash scripts/install-build-deps-debian.sh
```

The dependency script intentionally does not add a third-party Node repository. Install Node.js 24 LTS from a source you trust, then run:

```bash
bash scripts/check-host.sh
```

## Build modes

### 1. `target-files` — default

```bash
bash scripts/build-relief.sh
```

This performs the following:

1. Initializes the pinned GrapheneOS stable manifest.
2. Downloads GrapheneOS's current `allowed_signers` and verifies the manifest tag with Git/SSH signature verification.
3. Syncs the source tree.
4. Runs `adevtool generate-all -d tegu` to obtain/extract the Pixel vendor files.
5. Injects `ReliefSetup`.
6. Applies the small SetupWizard2 handoff patch.
7. Adds the Relief product fragment at the end of `aosp_tegu.mk`.
8. Selects `tegu-cur-user`.
9. Builds `target-files-package`.

The result is useful for checking whether the OS compiles, but **target-files are not the finished flashable production image**.

### 2. `dev` — fast hardware testing

```bash
RELIEF_BUILD_MODE=dev bash scripts/build-relief.sh
```

This runs the normal `m` target and exports the generated raw images. They use Android's public test keys.

**Never lock a Pixel bootloader around a `dev` Relief build.** Verified boot has no security value with public test keys. This mode is only for bring-up on a dedicated development phone.

### 3. `release` — signed daily-driver build

First generate persistent signing keys once:

```bash
SRC="$HOME/relief-build/grapheneos-2026081300"
bash scripts/generate-release-keys.sh "$SRC"
```

Every key prompt must use the same strong passphrase. Back up the entire `keys/tegu` directory in encrypted offline storage before relying on the OS. If these keys are lost, future Relief releases cannot update an installed Relief system under the same trust identity.

Then build the signed release:

```bash
RELIEF_BUILD_MODE=release bash scripts/build-relief.sh
```

The release path uses GrapheneOS's current production flow:

```text
m target-files-package
m otatools-package
script/finalize.sh
script/generate-release.sh tegu BUILD_NUMBER
```

It produces a signed factory install ZIP, signature, full update package and metadata. The generated GrapheneOS factory script includes the `avb_pkmd.bin` custom verified-boot key and flashes it into `avb_custom_key` as part of the install.

## Build numbers

By default Relief uses `YYYYMMDD00` in UTC. Override it explicitly when making multiple releases on the same date:

```bash
RELIEF_BUILD_NUMBER=2026082301 RELIEF_BUILD_MODE=release bash scripts/build-relief.sh
```

Do not reuse a build number for different published binaries.

## Parallelism

Relief caps the default build at 8 jobs to avoid stupid OOM failures on modest 32–64 GiB builders. Override when appropriate:

```bash
RELIEF_JOBS=16 bash scripts/build-relief.sh
```

More jobs are not automatically better. Android LTO/CFI linking can consume large amounts of RAM.

## Shallow source sync

For a disposable build host, Relief supports standard repo shallow cloning:

```bash
RELIEF_SHALLOW=1 bash scripts/build-relief.sh
```

This is an optimization, not required for correctness. If an upstream repository fails to sync shallowly, rerun using the normal sync rather than trying to patch around source-history problems.

## Updater policy

`OFFICIAL_BUILD` is deliberately unset. Relief therefore does **not** point a custom-signed derivative at GrapheneOS's official update service. GrapheneOS explicitly warns that doing so causes repeated downloads that can never validate with derivative signing keys.

For early Relief releases, update with signed full OTA packages or factory images. A dedicated Relief static update server can be added later.

## What the first profile removes

Only application-layer modules with verified current module names:

- `Auditor`
- `ExactCalculator`
- `LogViewer`
- `Messaging` — redundant because Relief requires Google Messages/RCS
- `PdfViewerGOS`
- `Traceur`

V1 intentionally retains items such as Seedvault, ThemePicker/WallpaperPicker, Camera, DocumentsUI, AppStore, Vanadium/WebView and all emergency/telephony/location infrastructure until dependency tests on real hardware show what can be removed safely.

## Failure policy

The builder stops instead of guessing when:

- the GrapheneOS manifest signature does not verify;
- the Relief SetupWizard patch no longer applies cleanly;
- the Pixel 9a product file has moved;
- required build resources are missing;
- release signing keys are missing in release mode.

That is intentional. A phone ROM should fail at build time rather than discover a bad assumption during an emergency call.