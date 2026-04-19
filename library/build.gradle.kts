import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.serialization)
    alias(libs.plugins.atomicfu)
}

group = properties["group"].toString()
version = properties["version"].toString()

val ratexHeaderDir = rootProject.file("external/RaTeX/crates/ratex-ffi/include")
val iosNativeDir = rootProject.file("native/ios")

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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "io.ratex.compose"
        compileSdk {
            version = release(36)
        }
        minSdk = 23

        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    applyDefaultHierarchyTemplate()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "RaTeX"
            isStatic = true
            linkerOpts(
                "-L${iosNativeDir.resolve(target.konanTarget.name).absolutePath}",
                "-lratex_ffi",
            )
        }
        target.compilations.getByName("main") {
            cinterops.create("ratex") {
                defFile(file("src/nativeInterop/cinterop/ratex.def"))
                includeDirs(ratexHeaderDir)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
        }
        jvmMain.dependencies {
            implementation(libs.jna)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    packageOfResClass = "io.ratex.compose.resources"
}

mavenPublishing {
    publishToMavenCentral()
    if (!project.hasProperty("skipSigning")) {
        signAllPublications()
    }
    coordinates(project.group.toString(), "ratex", project.version.toString())
    pom {
        val projectUrl = "https://github.com/darriousliu/RaTeX-CMP"
        val projectGitUrl = "$projectUrl.git"
        name.set("RaTeX Compose Multiplatform")
        description.set("Compose Multiplatform LaTeX math rendering powered by RaTeX")
        url.set(projectUrl)
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                name.set("RaTeX Contributors")
                url.set(projectUrl)
            }
        }
        scm {
            url.set(projectGitUrl)
            connection.set("scm:git:git://github.com/darriousliu/RaTeX-CMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/darriousliu/RaTeX-CMP.git")
        }
    }
}

dependencies {
    add("jvmTestRuntimeOnly", project(hostDesktopNativeProjectPath()))
}
