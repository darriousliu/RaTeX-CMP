import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.registering
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
val projectUrl = "https://github.com/darriousliu/RaTeX-CMP"
val projectGitUrl = "$projectUrl.git"
val projectScmConnection = "scm:git:git://github.com/darriousliu/RaTeX-CMP.git"
val projectScmDeveloperConnection = "scm:git:ssh://git@github.com/darriousliu/RaTeX-CMP.git"

data class DesktopNativeTarget(
    val jnaDir: String,
    val libraryFileName: String,
    val artifactId: String,
    val publicationName: String,
    val jarTaskName: String,
    val runtimeElementsName: String,
    val verifyTaskName: String,
    val supportedHostOs: Set<String>,
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
        supportedHostOs = setOf("darwin"),
    ),
    DesktopNativeTarget(
        jnaDir = "darwin-x86-64",
        libraryFileName = "libratex_ffi.dylib",
        artifactId = "ratex-compose-native-darwin-x86-64",
        publicationName = "desktopNativeDarwinX8664",
        jarTaskName = "desktopNativeDarwinX8664Jar",
        runtimeElementsName = "desktopNativeDarwinX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeDarwinX8664",
        supportedHostOs = setOf("darwin"),
    ),
    DesktopNativeTarget(
        jnaDir = "linux-aarch64",
        libraryFileName = "libratex_ffi.so",
        artifactId = "ratex-compose-native-linux-aarch64",
        publicationName = "desktopNativeLinuxAarch64",
        jarTaskName = "desktopNativeLinuxAarch64Jar",
        runtimeElementsName = "desktopNativeLinuxAarch64RuntimeElements",
        verifyTaskName = "verifyDesktopNativeLinuxAarch64",
        supportedHostOs = setOf("darwin", "linux"),
    ),
    DesktopNativeTarget(
        jnaDir = "linux-x86-64",
        libraryFileName = "libratex_ffi.so",
        artifactId = "ratex-compose-native-linux-x86-64",
        publicationName = "desktopNativeLinuxX8664",
        jarTaskName = "desktopNativeLinuxX8664Jar",
        runtimeElementsName = "desktopNativeLinuxX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeLinuxX8664",
        supportedHostOs = setOf("darwin", "linux"),
    ),
    DesktopNativeTarget(
        jnaDir = "windows-x86-64",
        libraryFileName = "ratex_ffi.dll",
        artifactId = "ratex-compose-native-windows-x86-64",
        publicationName = "desktopNativeWindowsX8664",
        jarTaskName = "desktopNativeWindowsX8664Jar",
        runtimeElementsName = "desktopNativeWindowsX8664RuntimeElements",
        verifyTaskName = "verifyDesktopNativeWindowsX8664",
        supportedHostOs = setOf("windows"),
    ),
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

fun hostDesktopNativeTarget(): DesktopNativeTarget {
    val hostOs = currentHostOs()
    val normalizedArch = when (val archName = System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> error("Unsupported desktop architecture: $archName")
    }

    val jnaDir = when (hostOs) {
        "darwin" -> "darwin-$normalizedArch"
        "linux" -> "linux-$normalizedArch"
        "windows" -> "windows-$normalizedArch"
        else -> error("Unsupported desktop OS: $hostOs")
    }
    return desktopNativeTargets.first { it.jnaDir == jnaDir }
}

fun supportedDesktopNativeTargetsForCurrentHost(): List<DesktopNativeTarget> {
    val hostOs = currentHostOs()
    return desktopNativeTargets.filter { hostOs in it.supportedHostOs }
}

fun unsupportedDesktopNativeTargetsForCurrentHost(): List<DesktopNativeTarget> {
    val supportedTargets = supportedDesktopNativeTargetsForCurrentHost().toSet()
    return desktopNativeTargets.filterNot { it in supportedTargets }
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

compose.resources {
    packageOfResClass = "io.ratex.compose.resources"
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

val prepareSupportedDesktopNativeArtifacts by tasks.registering(Exec::class) {
    group = "publishing"
    description = "Builds all desktop native artifacts supported on the current machine."
    workingDir(rootProject.rootDir)
    commandLine("bash", rootProject.file("prepare-jvm-rust.sh").absolutePath, "--all")
}

val supportedDesktopNativeVerifyTaskNames = supportedDesktopNativeTargetsForCurrentHost().map { it.verifyTaskName }

tasks.configureEach {
    if (name in supportedDesktopNativeVerifyTaskNames) {
        dependsOn(prepareSupportedDesktopNativeArtifacts)
    }
}

val verifySupportedDesktopNativeArtifacts by tasks.registering {
    group = "publishing"
    description = "Verifies all desktop native artifacts supported on the current machine."
    dependsOn(supportedDesktopNativeVerifyTaskNames)
}

fun supportedDesktopNativePublishTaskNamesFor(taskSuffix: String): List<String> =
    supportedDesktopNativeTargetsForCurrentHost().map { target ->
        "publish${target.publicationName.replaceFirstChar(Char::titlecase)}Publication$taskSuffix"
    }

fun unsupportedDesktopNativePublishTaskNamesFor(taskSuffix: String): List<String> =
    unsupportedDesktopNativeTargetsForCurrentHost().map { target ->
        "publish${target.publicationName.replaceFirstChar(Char::titlecase)}Publication$taskSuffix"
    }

val supportedDesktopNativePublishTaskNames = listOf(
    "ToMavenCentralRepository",
    "ToMavenLocal",
).flatMap(::supportedDesktopNativePublishTaskNamesFor)

val unsupportedDesktopNativePublishTaskNames = listOf(
    "ToMavenCentralRepository",
    "ToMavenLocal",
).flatMap(::unsupportedDesktopNativePublishTaskNamesFor)

tasks.configureEach {
    if (name in supportedDesktopNativePublishTaskNames) {
        dependsOn(verifySupportedDesktopNativeArtifacts)
    }
    if (name in unsupportedDesktopNativePublishTaskNames) {
        enabled = false
    }
}

val supportedDesktopNativePublishToMavenCentralRepositoryTaskNames =
    supportedDesktopNativePublishTaskNamesFor("ToMavenCentralRepository")

val publishSupportedDesktopNativePublicationsToMavenCentralRepository by tasks.registering {
    group = "publishing"
    description = "Builds, verifies, and publishes all desktop native publications supported on the current machine to the mavenCentral repository."
    dependsOn(supportedDesktopNativePublishToMavenCentralRepositoryTaskNames)
}

val supportedDesktopNativePublishToMavenLocalTaskNames =
    supportedDesktopNativePublishTaskNamesFor("ToMavenLocal")

val publishSupportedDesktopNativePublicationsToMavenLocal by tasks.registering {
    group = "publishing"
    description = "Builds, verifies, and publishes all desktop native publications supported on the current machine to Maven Local."
    dependsOn(supportedDesktopNativePublishToMavenLocalTaskNames)
}

val publishSupportedDesktopNativePublicationsToMavenCentral by tasks.registering {
    group = "publishing"
    description = "Alias for publishSupportedDesktopNativePublicationsToMavenCentralRepository."
    dependsOn(publishSupportedDesktopNativePublicationsToMavenCentralRepository)
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

publishing {
    publications {
        supportedDesktopNativeTargetsForCurrentHost().forEach { target ->
            create<MavenPublication>(target.publicationName) {
                artifactId = target.artifactId
                artifact(desktopNativeJarTasks.getValue(target))
                pom {
                    name.set("RaTeX Compose Native ${target.jnaDir}")
                    description.set("Desktop native runtime for RaTeX Compose Multiplatform (${target.jnaDir})")
                    url.set(projectUrl)
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    scm {
                        url.set(projectGitUrl)
                        connection.set(projectScmConnection)
                        developerConnection.set(projectScmDeveloperConnection)
                    }
                    developers {
                        developer {
                            name.set("RaTeX Contributors")
                            url.set(projectUrl)
                        }
                    }
                }
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (!project.hasProperty("skipSigning")) {
        signAllPublications()
    }

    pom {
        name.set("RaTeX Compose Multiplatform")
        description.set("Compose Multiplatform LaTeX math rendering powered by RaTeX")
        url.set(projectUrl)
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        scm {
            url.set(projectGitUrl)
            connection.set(projectScmConnection)
            developerConnection.set(projectScmDeveloperConnection)
        }
        developers {
            developer {
                name.set("RaTeX Contributors")
                url.set(projectUrl)
            }
        }
    }
}

dependencies {
    add("jvmTestRuntimeOnly", files(desktopNativeJarTasks.getValue(hostDesktopNativeTarget())))
}
