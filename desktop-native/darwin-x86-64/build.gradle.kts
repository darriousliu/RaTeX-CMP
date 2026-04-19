plugins {
    id("ratex.desktop-native-publish")
}

ratexDesktopNative {
    nativeTarget.set("darwin-x86-64")
    nativeFileName.set("libratex_ffi.dylib")
    artifactId.set("ratex-native-darwin-x86-64")
    supportedHostOs.set(setOf("darwin"))
}
