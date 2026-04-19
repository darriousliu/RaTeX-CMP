#!/usr/bin/env bash
# prepare-jvm-rust.sh — Build libratex_ffi for Compose Multiplatform JVM/Desktop
#
# Usage:
#   bash prepare-jvm-rust.sh
#   bash prepare-jvm-rust.sh --all
#   bash prepare-jvm-rust.sh darwin-aarch64
#   bash prepare-jvm-rust.sh linux-x86-64
#
# Output: library/native/{os-arch}/libratex_ffi.{dylib,so,dll}

set -eo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RATEX_ROOT="$PROJECT_ROOT/external/RaTeX"
NATIVE_DIR="$PROJECT_ROOT/library/native"

TARGETS=(
    "aarch64-apple-darwin         darwin-aarch64     libratex_ffi.dylib darwin"
    "x86_64-apple-darwin          darwin-x86-64      libratex_ffi.dylib darwin"
    "aarch64-unknown-linux-gnu    linux-aarch64      libratex_ffi.so    darwin,linux"
    "x86_64-unknown-linux-gnu     linux-x86-64       libratex_ffi.so    darwin,linux"
    "x86_64-pc-windows-msvc       windows-x86-64     ratex_ffi.dll      windows"
)

detect_host_target() {
    rustc -vV | awk '/^host:/ { print $2 }'
}

detect_host_os() {
    local host_target="$1"
    case "$host_target" in
        *apple-darwin) echo "darwin" ;;
        *unknown-linux-gnu) echo "linux" ;;
        *pc-windows-msvc) echo "windows" ;;
        *)
            echo "Error: unsupported host target $host_target" >&2
            exit 1
            ;;
    esac
}

target_supported_on_host() {
    local supported_hosts="$1" host_os="$2"
    case ",$supported_hosts," in
        *",$host_os,"*) return 0 ;;
        *) return 1 ;;
    esac
}

find_target_entry() {
    local requested_target="$1"
    for entry in "${TARGETS[@]}"; do
        read -r rust_target jna_dir lib_file supported_hosts <<< "$entry"
        if [ "$requested_target" = "$jna_dir" ] || [ "$requested_target" = "$rust_target" ]; then
            printf '%s\n' "$rust_target $jna_dir $lib_file $supported_hosts"
            return 0
        fi
    done

    echo "Error: unsupported JVM desktop target '$requested_target'" >&2
    echo "Supported targets:" >&2
    for entry in "${TARGETS[@]}"; do
        read -r _ jna_dir _ _ <<< "$entry"
        echo "  - $jna_dir" >&2
    done
    exit 1
}

needs_zig_for_target() {
    local target="$1" host_target="$2" host_os="$3"
    if [ "$target" = "$host_target" ]; then
        return 1
    fi

    case "$target" in
        *unknown-linux-gnu) return 0 ;;
        *apple-darwin)
            if [ "$host_os" = "darwin" ]; then
                return 1
            fi
            ;;
    esac

    return 1
}

build_target() {
    local rust_target="$1" host_target="$2" host_os="$3"

    if [ "$rust_target" = "$host_target" ]; then
        cargo build --manifest-path "$RATEX_ROOT/Cargo.toml" \
            --release -p ratex-ffi --target "$rust_target"
        return
    fi

    case "$rust_target" in
        *apple-darwin)
            if [ "$host_os" != "darwin" ]; then
                echo "Error: $host_os host cannot build Apple desktop target $rust_target" >&2
                exit 1
            fi
            cargo build --manifest-path "$RATEX_ROOT/Cargo.toml" \
                --release -p ratex-ffi --target "$rust_target"
            ;;
        *unknown-linux-gnu)
            cargo zigbuild --manifest-path "$RATEX_ROOT/Cargo.toml" \
                --release -p ratex-ffi --target "$rust_target"
            ;;
        *)
            echo "Error: unsupported cross-compilation route from $host_target to $rust_target" >&2
            exit 1
            ;;
    esac
}

copy_lib() {
    local rust_target="$1" jna_dir="$2" lib_file="$3"
    local src="$RATEX_ROOT/target/$rust_target/release/$lib_file"
    local dest="$NATIVE_DIR/$jna_dir"

    if [ ! -f "$src" ]; then
        echo "    ✗ $rust_target — $src not found" >&2
        return 1
    fi
    mkdir -p "$dest"
    cp "$src" "$dest/"
    echo "    ✓ $rust_target → $dest/$lib_file"
}

build_host() {
    local host_target
    host_target="$(detect_host_target)"
    local host_os
    host_os="$(detect_host_os "$host_target")"

    echo "==> Building ratex-ffi for host: $host_target"
    build_target "$host_target" "$host_target" "$host_os"

    for entry in "${TARGETS[@]}"; do
        read -r target jna_dir lib_file _ <<< "$entry"
        if [ "$target" = "$host_target" ]; then
            copy_lib "$target" "$jna_dir" "$lib_file"
            return
        fi
    done

    echo "==> Error: host target $host_target not in supported list" >&2
    exit 1
}

build_one() {
    local requested_target="$1"
    local host_target host_os target jna_dir lib_file supported_hosts
    host_target="$(detect_host_target)"
    host_os="$(detect_host_os "$host_target")"
    read -r target jna_dir lib_file supported_hosts <<< "$(find_target_entry "$requested_target")"

    if ! target_supported_on_host "$supported_hosts" "$host_os"; then
        echo "Error: target $jna_dir is not supported on host $host_target" >&2
        exit 1
    fi

    if needs_zig_for_target "$target" "$host_target" "$host_os"; then
        command -v cargo-zigbuild >/dev/null 2>&1 || { echo "Error: cargo-zigbuild not found. Install with: cargo install cargo-zigbuild" >&2; exit 1; }
        command -v zig >/dev/null 2>&1 || { echo "Error: zig not found. Install with: brew install zig / apt install zig" >&2; exit 1; }
    fi

    rustup target add "$target"

    echo "==> Building ratex-ffi for target: $target ($jna_dir)"
    build_target "$target" "$host_target" "$host_os"
    copy_lib "$target" "$jna_dir" "$lib_file"
    echo "==> Done. Library copied to $NATIVE_DIR/$jna_dir"
}

build_all() {
    local host_target host_os
    host_target="$(detect_host_target)"
    host_os="$(detect_host_os "$host_target")"

    local selected_targets=()
    local targets_to_add=()
    for entry in "${TARGETS[@]}"; do
        read -r target _ _ supported_hosts <<< "$entry"
        if target_supported_on_host "$supported_hosts" "$host_os"; then
            selected_targets+=("$entry")
            targets_to_add+=("$target")
        fi
    done

    if [ "${#selected_targets[@]}" -eq 0 ]; then
        echo "Error: no JVM desktop targets are configured for host $host_target" >&2
        exit 1
    fi

    local needs_zig=0
    for entry in "${selected_targets[@]}"; do
        read -r target _ _ _ <<< "$entry"
        if needs_zig_for_target "$target" "$host_target" "$host_os"; then
            needs_zig=1
            break
        fi
    done

    if [ "$needs_zig" -eq 1 ]; then
        command -v cargo-zigbuild >/dev/null 2>&1 || { echo "Error: cargo-zigbuild not found. Install with: cargo install cargo-zigbuild" >&2; exit 1; }
        command -v zig >/dev/null 2>&1 || { echo "Error: zig not found. Install with: brew install zig / apt install zig" >&2; exit 1; }
    fi
    rustup target add "${targets_to_add[@]}"

    echo "==> Building ratex-ffi for host $host_target"
    echo "==> Selected JVM desktop targets supported on this machine:"
    for entry in "${selected_targets[@]}"; do
        read -r target jna_dir _ _ <<< "$entry"
        echo "    - $target ($jna_dir)"
    done
    echo "==> Starting builds (parallel)..."

    local pids=()
    for entry in "${selected_targets[@]}"; do
        read -r target jna_dir lib_file _ <<< "$entry"
        echo "    → $target [starting]"
        (
            build_target "$target" "$host_target" "$host_os"
            copy_lib "$target" "$jna_dir" "$lib_file"
        ) &
        pids+=($!)
    done

    local failed=0
    for pid in "${pids[@]}"; do
        wait "$pid" || failed=1
    done

    if [ "$failed" -ne 0 ]; then
        echo "==> Build failed!" >&2
        exit 1
    fi
    echo "==> Done. Libraries copied to $NATIVE_DIR"
}

case "${1:-}" in
    --all) build_all ;;
    "")    build_host ;;
    *)     build_one "$1" ;;
esac
