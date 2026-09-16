#!/usr/bin/env bash
# Per-boot: ensure UI preview static server is running (port 8765).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT=8765
PID_FILE="/tmp/prefixshield-preview.pid"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  exit 0
fi

cd "$ROOT/preview"
nohup python3 -m http.server "$PORT" --bind 127.0.0.1 \
  > /tmp/prefixshield-preview.log 2>&1 &
echo $! > "$PID_FILE"
