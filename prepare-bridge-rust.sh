#!/usr/bin/env bash
# Build the CMP-owned bridge next to the unchanged upstream native library.
# Usage: bash prepare-bridge-rust.sh <rust-target> <output-directory> [native|ios|android|zig|wasm]
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
TARGET="${1:?Rust target required}"
DEST="${2:?Output directory required}"
MODE="${3:-native}"
MANIFEST="$PROJECT_ROOT/rust-bridge/Cargo.toml"

case "$MODE" in
    ios)
        cargo +"${RATEX_IOS_RUST_TOOLCHAIN:-nightly-2026-08-16}" build \
            --manifest-path "$MANIFEST" --locked --release --target "$TARGET" \
            -Z build-std=std,panic_abort
        ;;
    android)
        (cd "$PROJECT_ROOT/rust-bridge" && cargo ndk --target "$TARGET" build --locked --release)
        ;;
    zig)
        cargo zigbuild --manifest-path "$MANIFEST" --locked --release --target "$TARGET"
        ;;
    native|wasm)
        if [ "$MODE" = wasm ]; then rustup target add "$TARGET"; fi
        cargo build --manifest-path "$MANIFEST" --locked --release --target "$TARGET"
        ;;
    *) echo "Unknown bridge build mode: $MODE" >&2; exit 1 ;;
esac

BUILT="$PROJECT_ROOT/rust-bridge/target/$TARGET/release"
mkdir -p "$DEST"
if [ "$MODE" = wasm ]; then
    cp "$BUILT/ratex_cmp_bridge.wasm" "$DEST/"
else
    case "$MODE:$TARGET" in
        ios:*) LIBRARIES=(libratex_cmp_bridge.a) ;;
        android:*) LIBRARIES=(libratex_cmp_bridge.so) ;;
        *:*-apple-darwin) LIBRARIES=(libratex_cmp_bridge.a libratex_cmp_bridge.dylib) ;;
        *:*-windows-*) LIBRARIES=(ratex_cmp_bridge.dll) ;;
        *) LIBRARIES=(libratex_cmp_bridge.so) ;;
    esac
    for name in "${LIBRARIES[@]}"; do
        cp "$BUILT/$name" "$DEST/"
    done
    if [ "$MODE" = ios ]; then
        bash "$PROJECT_ROOT/external/RaTeX/scripts/verify-apple-static-libraries.sh" "$DEST/libratex_cmp_bridge.a"
    fi
fi
