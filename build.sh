#!/usr/bin/env bash
set -euo pipefail

# --- Paths ---
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_ROOT/src"
BUILD_DIR="${BUILD_DIR:-$PROJECT_ROOT/build}"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_OUT="$BUILD_DIR/clearcanopy.jar"

# Per-machine overrides — create build.local (gitignored) to set PZ_DIR etc.
# See build.local.example for the template.
if [ -f "$PROJECT_ROOT/build.local" ]; then
    # shellcheck disable=SC1091
    source "$PROJECT_ROOT/build.local"
fi

if [ -z "${PZ_DIR:-}" ] || [ ! -f "$PZ_DIR/projectzomboid.jar" ]; then
    echo "[build] ERROR: Project Zomboid install not found." >&2
    echo "        Set PZ_DIR in build.local, e.g.:" >&2
    echo "          cp build.local.example build.local" >&2
    echo "          # then edit build.local to match your Steam install" >&2
    exit 1
fi

# If PZ is running, the JVM holds a Windows file lock on clearcanopy.jar.
# The install step below does `rm -rf` followed by `cp -r` — rm fails on
# the locked JAR mid-tree and `set -e` exits before cp runs, leaving the
# deployed mod folder half-empty. Pre-flight check aborts cleanly instead.
if [ -z "${SKIP_PZ_CHECK:-}" ] && command -v tasklist >/dev/null 2>&1; then
    if tasklist //FI "IMAGENAME eq ProjectZomboid64.exe" //FO CSV //NH 2>/dev/null | grep -qi ProjectZomboid64; then
        echo "[build] ERROR: Project Zomboid is running. Close it before building." >&2
        exit 1
    fi
fi

: "${MOD_INSTALL_ROOT:=$USERPROFILE/Zomboid/mods/ClearCanopy}"

PZ_JAR="$PZ_DIR/projectzomboid.jar"
ZB_JAR="$PZ_DIR/ZombieBuddy.jar"

# JDK: set JDK_DIR in build.local, or drop a Zulu JDK into tools/.
if [ -z "${JDK_DIR:-}" ]; then
    JDK_DIR="$(ls -d "$PROJECT_ROOT"/tools/zulu*-win_x64 2>/dev/null | head -n 1)"
fi
if [ -z "$JDK_DIR" ] || [ ! -f "$JDK_DIR/bin/javac.exe" ]; then
    echo "[build] ERROR: no JDK found. Set JDK_DIR in build.local or put a" >&2
    echo "        Zulu JDK 25 Windows x64 build under tools/." >&2
    exit 1
fi
JAVAC="$JDK_DIR/bin/javac.exe"
JAR="$JDK_DIR/bin/jar.exe"

# --- Clean ---
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

# --- Compile ---
echo "[build] Compiling..."
mapfile -t SOURCES < <(find "$SRC_DIR" -name '*.java')

"$JAVAC" \
    --release 17 \
    -classpath "$PZ_JAR;$ZB_JAR" \
    -d "$CLASSES_DIR" \
    "${SOURCES[@]}"

# --- Bundle LICENSE into jar (root + META-INF for tooling conventions) ---
mkdir -p "$CLASSES_DIR/META-INF"
cp "$PROJECT_ROOT/LICENSE" "$CLASSES_DIR/LICENSE"
cp "$PROJECT_ROOT/LICENSE" "$CLASSES_DIR/META-INF/LICENSE"

# --- Package jar ---
echo "[build] Packaging jar..."
"$JAR" --create --file "$JAR_OUT" -C "$CLASSES_DIR" .

# --- Stage mod directory ---
echo "[build] Staging mod directory..."
STAGE="$BUILD_DIR/stage/ClearCanopy"
rm -rf "$STAGE"
mkdir -p "$STAGE/42.20/media/java/client"
cp "$PROJECT_ROOT/mod_files/mod.info" "$STAGE/mod.info"
cp "$PROJECT_ROOT/mod_files/42.20/mod.info" "$STAGE/42.20/mod.info"
cp "$PROJECT_ROOT/poster.png" "$STAGE/poster.png"
cp "$PROJECT_ROOT/poster.png" "$STAGE/42.20/poster.png"
cp "$PROJECT_ROOT/icon.png" "$STAGE/icon.png"
cp "$PROJECT_ROOT/icon.png" "$STAGE/42.20/icon.png"
cp "$JAR_OUT" "$STAGE/42.20/media/java/client/clearcanopy.jar"

# --- Install to Zomboid mods dir ---
echo "[build] Installing to $MOD_INSTALL_ROOT"
rm -rf "$MOD_INSTALL_ROOT"
mkdir -p "$(dirname "$MOD_INSTALL_ROOT")"
cp -r "$STAGE" "$MOD_INSTALL_ROOT"

# --- Sync to Workshop staging dir, if configured ---
# PZ prefers the Workshop upload staging tree over ~/Zomboid/mods/, so
# both copies must stay in sync while testing. Point WORKSHOP_STAGE_MOD
# at the Contents/mods/ClearCanopy dir; the parent (with workshop.txt
# and preview.png) is preserved.
if [ -n "${WORKSHOP_STAGE_MOD:-}" ]; then
    echo "[build] Syncing to Workshop stage: $WORKSHOP_STAGE_MOD"
    rm -rf "$WORKSHOP_STAGE_MOD"
    mkdir -p "$(dirname "$WORKSHOP_STAGE_MOD")"
    cp -r "$STAGE" "$WORKSHOP_STAGE_MOD"
fi

echo "[build] Done."
echo "       Jar:     $JAR_OUT"
echo "       Install: $MOD_INSTALL_ROOT"
if [ -n "${WORKSHOP_STAGE_MOD:-}" ]; then
    echo "       WS:      $WORKSHOP_STAGE_MOD"
fi
