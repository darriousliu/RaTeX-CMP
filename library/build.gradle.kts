import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
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
val desktopNativeTargetAttribute = Attribute.of("io.ratex.desktop.native.target", String::class.java)

data class DesktopNativeTarget(
    val jnaDir: String,
    val libraryFileName: String,
    val artifactId: String,
    val publicationName: String,
    val jarTaskName: String,
    val runtimeElementsName: String,
    val verifyTaskName: String,
)

val desktopNativeTargets = listOf(
    DesktopNativeTarget(
        jnaDir = "darwin-aarch64",
        libraryFileName = "libratex_ffi.dylib",
        artifactId = "ratex-compose-native-darwin-aarch64",
        publicationName = "desktopNativeDarwinAarch64",
        jarTaskName = "desktopNativeDarwinAarch64Jar",
        runtimeElementsName = "desktopNativeDarwinAarch64RuntimeElements",
        verifyTaskName = "verifyDesktopNativeDarwinAarch64",
    ),
    DesktopNativeTarget(
        jnaDir = "darwin-x86-64",
        libraryFileName = "libratex_ffi.dylib",
        artifactId = "ratex-compose-native-darwin-x86-64",
        publicationName = "desktopNativeDarwinX8664",
        jarTaskName = "desktopNativeDarwinX8664Jar",
        runtimeElementsName = "desktopNativeDarwinX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeDarwinX8664",
    ),
    DesktopNativeTarget(
        jnaDir = "linux-aarch64",
        libraryFileName = "libratex_ffi.so",
        artifactId = "ratex-compose-native-linux-aarch64",
        publicationName = "desktopNativeLinuxAarch64",
        jarTaskName = "desktopNativeLinuxAarch64Jar",
        runtimeElementsName = "desktopNativeLinuxAarch64RuntimeElements",
        verifyTaskName = "verifyDesktopNativeLinuxAarch64",
    ),
    DesktopNativeTarget(
        jnaDir = "linux-x86-64",
        libraryFileName = "libratex_ffi.so",
        artifactId = "ratex-compose-native-linux-x86-64",
        publicationName = "desktopNativeLinuxX8664",
        jarTaskName = "desktopNativeLinuxX8664Jar",
        runtimeElementsName = "desktopNativeLinuxX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeLinuxX8664",
    ),
    DesktopNativeTarget(
        jnaDir = "windows-x86-64",
        libraryFileName = "ratex_ffi.dll",
        artifactId = "ratex-compose-native-windows-x86-64",
        publicationName = "desktopNativeWindowsX8664",
        jarTaskName = "desktopNativeWindowsX8664Jar",
        runtimeElementsName = "desktopNativeWindowsX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeWindowsX8664",
    ),
)

fun hostDesktopNativeTarget(): DesktopNativeTarget {
    val osName = System.getProperty("os.name").lowercase()
    val normalizedArch = when (val archName = System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> error("Unsupported desktop architecture: $archName")
    }

    val jnaDir = when {
        "mac" in osName -> "darwin-$normalizedArch"
        "linux" in osName -> "linux-$normalizedArch"
        "windows" in osName -> "windows-$normalizedArch"
        else -> error("Unsupported desktop OS: $osName")
    }
    return desktopNativeTargets.first { it.jnaDir == jnaDir }
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

val desktopNativeJarTasks = desktopNativeTargets.associateWith { target ->
    val verifyTask = tasks.register(target.verifyTaskName) {
        doLast {
            val libraryFile = file("native/${target.jnaDir}/${target.libraryFileName}")
            check(libraryFile.exists()) {
                "Missing desktop native library: ${libraryFile.absolutePath}. Run prepare-jvm-rust.sh before building/publishing."
            }
            check(libraryFile.length() > 0L) {
                "Desktop native library is empty: ${libraryFile.absolutePath}. Rebuild with prepare-jvm-rust.sh before building/publishing."
            }
        }
    }

    tasks.register<Jar>(target.jarTaskName) {
        dependsOn(verifyTask)
        archiveBaseName.set(target.artifactId)
        archiveVersion.set(project.version.toString())
        from(layout.projectDirectory.file("native/${target.jnaDir}/${target.libraryFileName}")) {
            into("META-INF/ratex/native/${target.jnaDir}")
        }
    }
}

desktopNativeTargets.forEach { target ->
    configurations.create(target.runtimeElementsName) {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(desktopNativeTargetAttribute, target.jnaDir)
        }
        outgoing.artifact(desktopNativeJarTasks.getValue(target))
    }
}

extensions.configure<PublishingExtension> {
    publications {
        desktopNativeTargets.forEach { target ->
            create<MavenPublication>(target.publicationName) {
                artifactId = target.artifactId
                artifact(desktopNativeJarTasks.getValue(target))
                pom {
                    name.set("RaTeX Compose Native ${target.jnaDir}")
                    description.set("Desktop native runtime for RaTeX Compose Multiplatform (${target.jnaDir})")
                    url.set("https://github.com/erweixin/RaTeX")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    scm {
                        url.set("https://github.com/erweixin/RaTeX")
                        connection.set("scm:git:git://github.com/erweixin/RaTeX.git")
                        developerConnection.set("scm:git:ssh://git@github.com/erweixin/RaTeX.git")
                    }
                    developers {
                        developer {
                            name.set("RaTeX Contributors")
                            url.set("https://github.com/erweixin/RaTeX")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    add("jvmTestRuntimeOnly", files(desktopNativeJarTasks.getValue(hostDesktopNativeTarget())))
}
