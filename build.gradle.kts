plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.atomicfu) apply false
}

data class DesktopNativeModule(
    val projectPath: String,
    val supportedHostOs: Set<String>,
)

fun currentHostOs(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        "mac" in osName -> "darwin"
        "linux" in osName -> "linux"
        "windows" in osName -> "windows"
        else -> error("Unsupported desktop OS: $osName")
    }
}

val desktopNativeModules = listOf(
    DesktopNativeModule(":desktop-native:darwin-aarch64", setOf("darwin")),
    DesktopNativeModule(":desktop-native:darwin-x86-64", setOf("darwin")),
    DesktopNativeModule(":desktop-native:linux-aarch64", setOf("darwin", "linux")),
    DesktopNativeModule(":desktop-native:linux-x86-64", setOf("darwin", "linux")),
    DesktopNativeModule(":desktop-native:windows-x86-64", setOf("windows")),
)

val supportedDesktopNativeModulePaths = desktopNativeModules
    .filter { currentHostOs() in it.supportedHostOs }
    .map { it.projectPath }

tasks.register("prepareSupportedDesktopNativeArtifacts") {
    group = "publishing"
    description = "Builds all desktop native artifacts supported on the current machine."
    dependsOn(supportedDesktopNativeModulePaths.map { "$it:prepareNativeArtifact" })
}

tasks.register("publishSupportedDesktopNativePublicationsToMavenLocal") {
    group = "publishing"
    description = "Publishes all desktop native libraries supported on the current machine to Maven Local."
    dependsOn(supportedDesktopNativeModulePaths.map { "$it:publishToMavenLocal" })
}

tasks.register("publishSupportedDesktopNativePublicationsToMavenCentralRepository") {
    group = "publishing"
    description = "Publishes all desktop native libraries supported on the current machine to the mavenCentral repository."
    dependsOn(supportedDesktopNativeModulePaths.map { "$it:publishAllPublicationsToMavenCentralRepository" })
}

tasks.register("publishSupportedDesktopNativePublicationsToMavenCentral") {
    group = "publishing"
    description = "Alias for publishSupportedDesktopNativePublicationsToMavenCentralRepository."
    dependsOn("publishSupportedDesktopNativePublicationsToMavenCentralRepository")
}
