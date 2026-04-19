plugins {
    id("ratex.desktop-native-publish")
}

ratexDesktopNative {
    nativeTarget.set("windows-x86-64")
    nativeFileName.set("ratex_ffi.dll")
    artifactId.set("ratex-native-windows-x86-64")
    supportedHostOs.set(setOf("windows"))
}
