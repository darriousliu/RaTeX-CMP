plugins {
    id("ratex.desktop-native-publish")
}

ratexDesktopNative {
    nativeTarget.set("linux-x86-64")
    nativeFileName.set("libratex_ffi.so")
    artifactId.set("ratex-native-linux-x86-64")
    supportedHostOs.set(setOf("darwin", "linux"))
}
