#!/usr/bin/env bash
set -euo pipefail

RELIEF_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SRC=${1:-}

[[ -n "$SRC" ]] || { echo "usage: $0 /path/to/grapheneos-source" >&2; exit 2; }
SRC=$(cd "$SRC" && pwd)

[[ -f "$SRC/build/envsetup.sh" ]] || { echo "Not an Android source tree: $SRC" >&2; exit 1; }
[[ -d "$SRC/packages/apps/SetupWizard2" ]] || { echo "GrapheneOS SetupWizard2 not found in $SRC" >&2; exit 1; }
[[ -f "$SRC/device/google/tegu/aosp_tegu.mk" ]] || { echo "Pixel 9a product file not found: device/google/tegu/aosp_tegu.mk" >&2; exit 1; }

printf 'Injecting ReliefSetup...\n'
rm -rf "$SRC/packages/apps/ReliefSetup"
mkdir -p "$SRC/packages/apps/ReliefSetup"
cp -a "$RELIEF_ROOT/android/ReliefSetup/." "$SRC/packages/apps/ReliefSetup/"

printf 'Injecting Relief product fragment...\n'
mkdir -p "$SRC/vendor/relief"
cp "$RELIEF_ROOT/product/relief.mk" "$SRC/vendor/relief/relief.mk"

printf 'Patching GrapheneOS SetupWizard2...\n'
SW="$SRC/packages/apps/SetupWizard2"
PATCH="$RELIEF_ROOT/patches/setupwizard-relief.patch"
if git -C "$SW" apply --check "$PATCH" >/dev/null 2>&1; then
    git -C "$SW" apply "$PATCH"
elif git -C "$SW" apply --reverse --check "$PATCH" >/dev/null 2>&1; then
    echo "SetupWizard2 Relief patch already applied."
else
    echo "ERROR: Relief setup-wizard patch no longer applies cleanly to this GrapheneOS source." >&2
    echo "Refusing to continue. Rebase patches/setupwizard-relief.patch against the selected base." >&2
    exit 1
fi

PRODUCT="$SRC/device/google/tegu/aosp_tegu.mk"
INHERIT='$(call inherit-product, vendor/relief/relief.mk)'
if ! grep -Fq "$INHERIT" "$PRODUCT"; then
    cat >> "$PRODUCT" <<'EOF'

# Relief minimal-phone profile. Keep this last so application-layer removals
# happen after the base Pixel/GrapheneOS product is assembled.
$(call inherit-product, vendor/relief/relief.mk)
EOF
else
    echo "Relief product fragment already inherited."
fi

printf '\nRelief applied to %s\n' "$SRC"
printf 'Sanity checks:\n'
grep -Fn 'vendor/relief/relief.mk' "$PRODUCT"
grep -Fn 'app.relief.setup' "$SRC/packages/apps/SetupWizard2/java/app/grapheneos/setupwizard/view/activity/FinishActivity.kt"
