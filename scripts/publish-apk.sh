#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source env.sh
./gradlew assembleRelease --no-daemon --max-workers=1
APK="app/build/outputs/apk/release/app-release.apk"
VERSION="$(python3 - <<'PY'
import re, pathlib
text = pathlib.Path("app/build.gradle.kts").read_text()
code = re.search(r'versionCode\s*=\s*(\d+)', text).group(1)
name = re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)
print(f"{name}|{code}")
PY
)"
NAME="${VERSION%%|*}"
CODE="${VERSION##*|}"
TAG="v${NAME}"
NOTES="$(cat <<EOF
versionCode: ${CODE}
versionName: ${NAME}

Fixed call history, dialer, contact form, and slide-to-answer calls.
EOF
)"
gh release delete "$TAG" -y >/dev/null 2>&1 || true
gh release create "$TAG" "$APK" --title "myPhone $NAME" --notes "$NOTES"
python3 - "$CODE" "$NAME" "$TAG" <<'PY'
import json
import sys

code, name, tag = sys.argv[1:]
manifest = {
    "versionCode": int(code),
    "versionName": name,
    "apkUrl": f"https://github.com/sahzadahmad246/prefixsheild/releases/download/{tag}/app-release.apk",
    "notes": "Fixed call history, dialer, contact form, and slide-to-answer calls.",
}
with open("update.json", "w", encoding="utf-8") as out:
    json.dump(manifest, out, indent=2)
    out.write("\n")
PY
echo "Release $TAG published. Commit and push update.json if it changed."
