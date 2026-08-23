#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "Run this script as root (for example: sudo $0)" >&2
    exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends \
    repo yarnpkg zip rsync \
    git git-lfs python3 gnupg openssh-client curl ca-certificates \
    diffutils fontconfig fonts-dejavu-core inetutils-hostname \
    openssl unzip gperf \
    libc6-dev-i386 lib32gcc-s1

cat <<'EOF'

Base packages installed.

GrapheneOS adevtool currently requires Node.js 24 LTS. Debian 12's stock
Node.js is older, so Relief intentionally does not replace your Node.js from
an unaudited third-party repository. Install Node.js 24 LTS from a source you
trust, then run:

  ./scripts/check-host.sh

On Debian the yarn executable supplied by the distribution is named yarnpkg;
the Relief builder handles that automatically.
EOF
