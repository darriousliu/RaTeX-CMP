import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

fun hostNativeRuntimeConfigurationName(): String {
    val osName = System.getProperty("os.name").lowercase()
    val normalizedArch = when (val archName = System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> error("Unsupported desktop architecture: $archName")
    }

    return when {
        "mac" in osName && normalizedArch == "aarch64" -> "desktopNativeDarwinAarch64RuntimeElements"
        "mac" in osName && normalizedArch == "x86-64" -> "desktopNativeDarwinX8664RuntimeElements"
        "linux" in osName && normalizedArch == "aarch64" -> "desktopNativeLinuxAarch64RuntimeElements"
        "linux" in osName && normalizedArch == "x86-64" -> "desktopNativeLinuxX8664RuntimeElements"
        "windows" in osName && normalizedArch == "x86-64" -> "desktopNativeWindowsX8664RuntimeElements"
        else -> error("Unsupported desktop OS: $osName")
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
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
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            runtimeOnly(
                project(
                    mapOf(
                        "path" to ":library",
                        "configuration" to hostNativeRuntimeConfigurationName(),
                    )
                )
            )
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.lifecycle.runtime.ktx)
        }
    }
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
