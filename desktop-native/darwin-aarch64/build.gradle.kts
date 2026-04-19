plugins {
    id("ratex.desktop-native-publish")
}

ratexDesktopNative {
    nativeTarget.set("darwin-aarch64")
    nativeFileName.set("libratex_ffi.dylib")
    artifactId.set("ratex-native-darwin-aarch64")
    supportedHostOs.set(setOf("darwin"))
}
