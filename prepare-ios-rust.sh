#!/usr/bin/env bash
# prepare-ios-rust.sh — Build Rust static libs for Kotlin/Native cinterop consumers.
#
# Uses the upstream pinned nightly and rebuilds std with panic_abort to avoid
# Rust unwind personality references in the final iOS application.
# Override the toolchain with RATEX_IOS_RUST_TOOLCHAIN when testing another nightly.
#
# Output layout:
#   native/ios/ios_arm64/libratex_ffi.a
#   native/ios/ios_simulator_arm64/libratex_ffi.a

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RATEX_ROOT="$PROJECT_ROOT/external/RaTeX"
OUT_DIR="$PROJECT_ROOT/native/ios"
IOS_RUST_TOOLCHAIN="${RATEX_IOS_RUST_TOOLCHAIN:-nightly-2026-08-16}"

TARGETS=(
  "aarch64-apple-ios         ios_arm64"
  "aarch64-apple-ios-sim     ios_simulator_arm64"
)

rustup toolchain install "$IOS_RUST_TOOLCHAIN" --profile minimal --component rust-src
rustup target add --toolchain "$IOS_RUST_TOOLCHAIN" aarch64-apple-ios aarch64-apple-ios-sim

echo "==> Building ratex-ffi static libraries for iOS KMP consumers..."
for entry in "${TARGETS[@]}"; do
  read -r RUST_TARGET KMP_DIR <<< "$entry"
  echo "    -> $RUST_TARGET"
  cargo +"$IOS_RUST_TOOLCHAIN" build \
    -Z build-std=std,panic_abort \
    --release \
    --manifest-path "$RATEX_ROOT/Cargo.toml" \
    -p ratex-ffi \
    --target "$RUST_TARGET"

  STATIC_LIB="$RATEX_ROOT/target/$RUST_TARGET/release/libratex_ffi.a"
  bash "$RATEX_ROOT/scripts/verify-apple-static-libraries.sh" "$STATIC_LIB"
  mkdir -p "$OUT_DIR/$KMP_DIR"
  cp "$STATIC_LIB" "$OUT_DIR/$KMP_DIR/"
done

echo "==> Done. Static libraries copied to $OUT_DIR"
