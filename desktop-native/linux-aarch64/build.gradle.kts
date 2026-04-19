plugins {
    id("ratex.desktop-native-publish")
}

ratexDesktopNative {
    nativeTarget.set("linux-aarch64")
    nativeFileName.set("libratex_ffi.so")
    artifactId.set("ratex-native-linux-aarch64")
    supportedHostOs.set(setOf("darwin", "linux"))
}
