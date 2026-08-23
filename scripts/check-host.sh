#!/usr/bin/env bash
set -euo pipefail

MIN_RAM_GIB="${RELIEF_MIN_RAM_GIB:-32}"
MIN_DISK_GIB="${RELIEF_MIN_DISK_GIB:-220}"
CHECK_PATH="${1:-${RELIEF_WORKDIR:-$HOME/relief-build}}"

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
warn() { printf 'WARN:  %s\n' "$*" >&2; }
ok()   { printf 'OK:    %s\n' "$*"; }

[[ "$(uname -s)" == "Linux" ]] || fail "GrapheneOS must be built on Linux."
[[ "$(uname -m)" == "x86_64" ]] || fail "GrapheneOS requires an x86-64 build host; found $(uname -m)."

mem_kib=$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)
mem_gib=$((mem_kib / 1024 / 1024))
(( mem_gib >= MIN_RAM_GIB )) || fail "Need at least ${MIN_RAM_GIB} GiB RAM; found about ${mem_gib} GiB."
ok "RAM: about ${mem_gib} GiB"

mkdir -p "$CHECK_PATH"
free_kib=$(df -Pk "$CHECK_PATH" | awk 'NR==2 {print $4}')
free_gib=$((free_kib / 1024 / 1024))
(( free_gib >= MIN_DISK_GIB )) || fail "Need at least ${MIN_DISK_GIB} GiB free at $CHECK_PATH; found about ${free_gib} GiB."
ok "free disk at $CHECK_PATH: about ${free_gib} GiB"

required=(git repo python3 gpg ssh-keygen curl rsync unzip zip openssl diff hostname)
for cmd in "${required[@]}"; do
    command -v "$cmd" >/dev/null 2>&1 || fail "Missing required command: $cmd"
done
ok "baseline build/fetch tools present"

if command -v node >/dev/null 2>&1; then
    node_major=$(node -p 'process.versions.node.split(".")[0]')
    (( node_major >= 24 )) || fail "adevtool requires Node.js 24 LTS; found $(node --version)."
    ok "Node.js $(node --version)"
else
    fail "Missing Node.js 24 LTS required by adevtool."
fi

if command -v yarn >/dev/null 2>&1; then
    YARN_BIN=$(command -v yarn)
elif command -v yarnpkg >/dev/null 2>&1; then
    YARN_BIN=$(command -v yarnpkg)
else
    fail "Missing yarn/yarnpkg required by adevtool."
fi
ok "yarn: $YARN_BIN"

for cmd in git-lfs gperf; do
    command -v "$cmd" >/dev/null 2>&1 || warn "$cmd not found. Official prebuilts may avoid rebuilding Vanadium, but install it for a complete GrapheneOS build host."
done

printf '\nHost passes Relief baseline checks.\n'
