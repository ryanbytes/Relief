#!/usr/bin/env bash
set -euo pipefail
umask 077

SRC=${1:-}
[[ -n "$SRC" ]] || { echo "usage: $0 /path/to/grapheneos-source" >&2; exit 2; }
SRC=$(cd "$SRC" && pwd)
[[ -f "$SRC/development/tools/make_key" ]] || { echo "GrapheneOS source tree not found: $SRC" >&2; exit 1; }

DEVICE=tegu
KEYDIR="$SRC/keys/$DEVICE"
CN="${RELIEF_SIGNING_CN:-Relief}"

if [[ -e "$KEYDIR/releasekey.pk8" || -e "$KEYDIR/avb.pem" ]]; then
    echo "Refusing to overwrite existing release keys in $KEYDIR" >&2
    exit 1
fi

cat <<EOF
Generating new permanent Relief signing keys for Pixel 9a ($DEVICE).

These keys define the device's update/verified-boot trust identity. Back them
up securely. Losing them means future builds cannot update an installed Relief
system without a factory reset.

Use the SAME strong passphrase for every key prompt, including the SSH key.
Certificate CN: $CN
EOF

read -r -p "Press Enter to continue, or Ctrl-C to abort. " _

mkdir -p "$KEYDIR"
cd "$KEYDIR"

keys=(bluetooth gmscompat_lib media networkstack nfc platform releasekey sdk_sandbox shared)
for key in "${keys[@]}"; do
    echo "Generating $key..."
    ../../development/tools/make_key "$key" "/CN=$CN/"
done

echo "Generating Android Verified Boot key..."
openssl genrsa 4096 | openssl pkcs8 -topk8 -scrypt -out avb.pem
../../external/avb/avbtool.py extract_public_key --key avb.pem --output avb_pkmd.bin

cd "$SRC"
echo "Generating factory-image signature key..."
ssh-keygen -t ed25519 -f "keys/$DEVICE/id_ed25519" -C "Relief Pixel 9a factory images"

cat <<EOF

Release keys generated in:
  $KEYDIR

Do NOT commit, upload, email, or casually copy the private keys.
Back up the complete directory in at least two encrypted offline locations.

The non-secret file used to provision Android Verified Boot is:
  $KEYDIR/avb_pkmd.bin

Next production build:
  RELIEF_BUILD_MODE=release RELIEF_WORKDIR="$(dirname "$SRC")" bash scripts/build-relief.sh
EOF
