#!/usr/bin/env bash
# prepare-android-rust.sh — Build libratex_ffi.so for Compose Multiplatform Android.
#
# Prerequisites:
#   cargo install cargo-ndk
#   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
#   NDK installed (set ANDROID_NDK_HOME or let cargo-ndk auto-detect)
#
# Output:
#   library/src/androidMain/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/libratex_ffi.so

set -eo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RATEX_ROOT="$PROJECT_ROOT/external/RaTeX"
JNILIBS="$PROJECT_ROOT/library/src/androidMain/jniLibs"

if ! cargo ndk --version >/dev/null 2>&1; then
    cat >&2 <<'EOF'
Error: cargo-ndk is not installed or is not available on PATH.

Install it with:
  cargo install cargo-ndk

If you are on Windows, also make sure Cargo's bin directory is on PATH before rerunning:
  %USERPROFILE%\.cargo\bin
EOF
    exit 1
fi

abi_for() {
    case "$1" in
        aarch64-linux-android) echo "arm64-v8a" ;;
        armv7-linux-androideabi) echo "armeabi-v7a" ;;
        x86_64-linux-android) echo "x86_64" ;;
        i686-linux-android) echo "x86" ;;
        *) echo "unknown target: $1" >&2; exit 1 ;;
    esac
}

echo "==> Building ratex-ffi for Compose Multiplatform Android targets..."
for rust_target in aarch64-linux-android armv7-linux-androideabi x86_64-linux-android; do
    abi="$(abi_for "$rust_target")"
    echo "    -> $rust_target ($abi)"
    (
        cd "$RATEX_ROOT"
        cargo ndk \
            --target "$rust_target" \
            build --release -p ratex-ffi
    )

    dest="$JNILIBS/$abi"
    mkdir -p "$dest"
    cp "$RATEX_ROOT/target/$rust_target/release/libratex_ffi.so" "$dest/"
done

echo "==> Done. Libraries copied to $JNILIBS"
