#!/usr/bin/env bash
# prepare-ios-rust.sh — Build Rust static libs for Kotlin/Native cinterop consumers.
#
# Output layout:
#   native/ios/ios_arm64/libratex_ffi.a
#   native/ios/ios_simulator_arm64/libratex_ffi.a
#   native/ios/ios_x64/libratex_ffi.a

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RATEX_ROOT="$PROJECT_ROOT/external/RaTeX"
OUT_DIR="$PROJECT_ROOT/native/ios"

TARGETS=(
  "aarch64-apple-ios         ios_arm64"
  "aarch64-apple-ios-sim     ios_simulator_arm64"
  "x86_64-apple-ios          ios_x64"
)

rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios

echo "==> Building ratex-ffi static libraries for iOS KMP consumers..."
for entry in "${TARGETS[@]}"; do
  read -r RUST_TARGET KMP_DIR <<< "$entry"
  echo "    -> $RUST_TARGET"
  cargo build \
    --release \
    --manifest-path "$RATEX_ROOT/Cargo.toml" \
    -p ratex-ffi \
    --target "$RUST_TARGET"

  mkdir -p "$OUT_DIR/$KMP_DIR"
  cp "$RATEX_ROOT/target/$RUST_TARGET/release/libratex_ffi.a" "$OUT_DIR/$KMP_DIR/"
done

echo "==> Done. Static libraries copied to $OUT_DIR"
