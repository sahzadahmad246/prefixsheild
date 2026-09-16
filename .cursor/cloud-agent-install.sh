#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for PrefixShield (Android CLI build).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
CMDLINE_TOOLS="$ANDROID_HOME/cmdline-tools/latest"
MARKER="$ANDROID_HOME/.prefixshield-bootstrap-v1"

install_jdk17() {
  local jdk_dir="$HOME/tools/jdk-17"
  if [[ -x "$jdk_dir/bin/java" ]]; then
    return 0
  fi
  mkdir -p "$HOME/tools"
  if [[ -d /usr/lib/jvm/java-17-openjdk-amd64 ]]; then
    ln -sfn /usr/lib/jvm/java-17-openjdk-amd64 "$jdk_dir"
    return 0
  fi
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends openjdk-17-jdk
  ln -sfn /usr/lib/jvm/java-17-openjdk-amd64 "$jdk_dir"
}

install_android_sdk() {
  if [[ -f "$MARKER" ]]; then
    return 0
  fi

  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    unzip wget ca-certificates

  mkdir -p "$ANDROID_HOME/cmdline-tools"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  wget -q -O "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  rm -rf "$CMDLINE_TOOLS"
  mv "$tmp/cmdline-tools" "$CMDLINE_TOOLS"

  # shellcheck disable=SC1091
  source "$ROOT/env.sh"
  set +o pipefail
  yes | sdkmanager --licenses >/dev/null || true
  set -o pipefail
  sdkmanager --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "build-tools;34.0.0"

  touch "$MARKER"
}

install_jdk17
install_android_sdk

# shellcheck disable=SC1091
source "$ROOT/env.sh"
./gradlew --no-daemon dependencies >/dev/null
