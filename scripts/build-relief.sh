#!/usr/bin/env bash
set -euo pipefail

RELIEF_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BASE_TAG="${RELIEF_BASE_TAG:-2026081300}"
DEVICE=tegu
MODE="${RELIEF_BUILD_MODE:-target-files}"
WORKDIR="${RELIEF_WORKDIR:-$HOME/relief-build}"
SRC="$WORKDIR/grapheneos-$BASE_TAG"
ARTIFACT_DIR="${RELIEF_ARTIFACT_DIR:-$WORKDIR/artifacts}"
BUILD_NUMBER="${RELIEF_BUILD_NUMBER:-$(date -u +%Y%m%d)00}"
JOBS="${RELIEF_JOBS:-$(nproc)}"
if (( JOBS > 8 )); then JOBS=8; fi

export PATH="$PATH:/sbin:/usr/sbin:/usr/local/sbin"
export BUILD_NUMBER
unset OFFICIAL_BUILD || true

case "$MODE" in
    dev|target-files|release) ;;
    *) echo "RELIEF_BUILD_MODE must be dev, target-files, or release" >&2; exit 2 ;;
esac

mkdir -p "$WORKDIR" "$ARTIFACT_DIR"
bash "$RELIEF_ROOT/scripts/check-host.sh" "$WORKDIR"

if command -v yarn >/dev/null 2>&1; then
    YARN=yarn
else
    YARN=yarnpkg
fi

mkdir -p "$SRC"
cd "$SRC"

if [[ ! -d .repo ]]; then
    echo "Initializing GrapheneOS $BASE_TAG..."
    if [[ "${RELIEF_SHALLOW:-0}" == "1" ]]; then
        repo init --depth=1 -u https://github.com/GrapheneOS/platform_manifest.git -b "refs/tags/$BASE_TAG"
    else
        repo init -u https://github.com/GrapheneOS/platform_manifest.git -b "refs/tags/$BASE_TAG"
    fi
else
    echo "Reusing dedicated Relief source tree and resetting prior generated modifications..."
    rm -rf packages/apps/ReliefSetup vendor/relief
    repo forall -c 'git reset --hard HEAD >/dev/null && git clean -fd >/dev/null' || true
    repo init -u https://github.com/GrapheneOS/platform_manifest.git -b "refs/tags/$BASE_TAG"
fi

mkdir -p "$HOME/.ssh"
curl --fail --silent --show-error https://grapheneos.org/allowed_signers > "$HOME/.ssh/grapheneos_allowed_signers"
(
    cd .repo/manifests
    git config gpg.ssh.allowedSignersFile "$HOME/.ssh/grapheneos_allowed_signers"
    echo "Verifying GrapheneOS manifest tag..."
    git verify-tag "$(git describe)"
)

echo "Syncing GrapheneOS source..."
repo sync -j8

# shellcheck disable=SC1091
source build/envsetup.sh

echo "Preparing adevtool dependencies..."
"$YARN" --cwd vendor/adevtool/ install

echo "Generating Pixel 9a vendor files..."
adevtool generate-all -d "$DEVICE"

echo "Applying Relief changes..."
bash "$RELIEF_ROOT/scripts/apply-relief.sh" "$SRC"

# Re-read build environment after adding the Relief module/product fragment.
# shellcheck disable=SC1091
source build/envsetup.sh
lunch tegu-cur-user

LOGDIR="$WORKDIR/logs"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/relief-${BUILD_NUMBER}-${MODE}.log"

echo "Building Relief: device=$DEVICE base=$BASE_TAG mode=$MODE build=$BUILD_NUMBER jobs=$JOBS"
echo "Log: $LOG"

case "$MODE" in
    dev)
        # Fast development image. Uses public Android test keys: never lock the
        # bootloader and never treat this as a secure daily-driver release.
        m -j"$JOBS" 2>&1 | tee "$LOG"
        OUT="$SRC/out/target/product/$DEVICE"
        DEST="$ARTIFACT_DIR/relief-$DEVICE-$BUILD_NUMBER-dev"
        rm -rf "$DEST"
        mkdir -p "$DEST"
        find "$OUT" -maxdepth 1 -type f \( -name '*.img' -o -name 'android-info.txt' \) -exec cp -a {} "$DEST/" \;
        ;;

    target-files)
        # Pixel 9a/tegu is not in GrapheneOS's current list requiring the
        # extra vendorbootimage/vendorkernelbootimage targets.
        m -j"$JOBS" target-files-package 2>&1 | tee "$LOG"
        DEST="$ARTIFACT_DIR/relief-$DEVICE-$BUILD_NUMBER-target-files"
        rm -rf "$DEST"
        mkdir -p "$DEST"
        find "$SRC/out" -type f -name '*target_files*.zip' -exec cp -a {} "$DEST/" \;
        ;;

    release)
        [[ -f "$SRC/keys/$DEVICE/avb.pem" ]] || {
            echo "Release keys are missing. Run scripts/generate-release-keys.sh $SRC first." >&2
            exit 1
        }
        [[ -f "$SRC/keys/$DEVICE/id_ed25519" ]] || {
            echo "Factory-image SSH signing key is missing." >&2
            exit 1
        }

        # Production path: user build -> target files -> otatools -> resign ->
        # signed factory image and full update package.
        rm -rf out
        m -j"$JOBS" target-files-package 2>&1 | tee "$LOG"
        m -j"$JOBS" otatools-package 2>&1 | tee -a "$LOG"
        script/finalize.sh 2>&1 | tee -a "$LOG"
        script/generate-release.sh "$DEVICE" "$BUILD_NUMBER" 2>&1 | tee -a "$LOG"

        RELEASE="$SRC/releases/$BUILD_NUMBER/release-$DEVICE-$BUILD_NUMBER"
        [[ -d "$RELEASE" ]] || { echo "Expected release directory not found: $RELEASE" >&2; exit 1; }
        DEST="$ARTIFACT_DIR/relief-$DEVICE-$BUILD_NUMBER-release"
        rm -rf "$DEST"
        cp -a "$RELEASE" "$DEST"
        cp -a "$SRC/keys/$DEVICE/avb_pkmd.bin" "$DEST/"
        ;;
esac

cat <<EOF

Relief build completed.
Mode:      $MODE
Base:      GrapheneOS $BASE_TAG
Device:    Pixel 9a ($DEVICE)
Build no.: $BUILD_NUMBER
Artifacts: $DEST
Log:       $LOG
EOF

if [[ "$MODE" == "dev" ]]; then
    echo "SECURITY: development artifacts use public test keys; keep the bootloader unlocked."
elif [[ "$MODE" == "target-files" ]]; then
    echo "Target-files are not a flashable production release until signed with your persistent Relief keys."
else
    echo "Signed release generated. Read docs/INSTALL.md before flashing or locking the bootloader."
fi
