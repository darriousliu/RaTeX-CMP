import org.gradle.api.tasks.Copy
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

fun hostDesktopNativeProjectPath(): String {
    val osName = System.getProperty("os.name").lowercase()
    val normalizedArch = when (val archName = System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> error("Unsupported desktop architecture: $archName")
    }

    return when {
        "mac" in osName && normalizedArch == "aarch64" -> ":desktop-native:darwin-aarch64"
        "mac" in osName && normalizedArch == "x86-64" -> ":desktop-native:darwin-x86-64"
        "linux" in osName && normalizedArch == "aarch64" -> ":desktop-native:linux-aarch64"
        "linux" in osName && normalizedArch == "x86-64" -> ":desktop-native:linux-x86-64"
        "windows" in osName && normalizedArch == "x86-64" -> ":desktop-native:windows-x86-64"
        else -> error("Unsupported desktop OS: $osName")
    }
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "io.ratex.compose.example.main"
        }
    }

    js(IR) {
        useEsModules()
        browser()
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    android {
        namespace = "io.ratex.compose.example"
        compileSdk {
            version = release(36)
        }
        minSdk = 23

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.library)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            runtimeOnly(project(hostDesktopNativeProjectPath()))
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.lifecycle.runtime.ktx)
        }
    }
}

compose.resources {
    packageOfResClass = "io.ratex.compose.example.resources"
}

// Compose does not bundle resources for standalone macOS Native executables yet.
// Keep this sample-only bridge until the upstream executable resource pipeline supports it.
val prepareMacosArm64ExecutableResources = tasks.register<Copy>("prepareMacosArm64ExecutableResources") {
    description = "Copies Compose resources next to the macosArm64 Native executable."
    dependsOn("macosArm64ProcessResources", "linkDebugExecutableMacosArm64")
    from(layout.buildDirectory.dir("processedResources/macosArm64/main/composeResources")) {
        into("composeResources")
    }
    into(layout.buildDirectory.dir("bin/macosArm64/debugExecutable/compose-resources"))
}

tasks.named("runDebugExecutableMacosArm64") {
    dependsOn(prepareMacosArm64ExecutableResources)
}

compose.desktop {
    application {
        mainClass = "io.ratex.compose.example.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.ratex.compose.example"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
