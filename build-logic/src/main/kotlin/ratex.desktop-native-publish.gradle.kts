import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.ratex.buildlogic.RaTeXDesktopNativeExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

group = properties["group"].toString()
version = properties["version"].toString()

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val ratexDesktopNative = extensions.create<RaTeXDesktopNativeExtension>("ratexDesktopNative")
ratexDesktopNative.supportedHostOs.convention(emptySet())

fun currentHostOs(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        "mac" in osName -> "darwin"
        "linux" in osName -> "linux"
        "windows" in osName -> "windows"
        else -> error("Unsupported desktop OS: $osName")
    }
}

fun Project.bashExecutable(): String {
    if (currentHostOs() != "windows") return "bash"

    val candidates = listOf(
        file("C:/Program Files/Git/bin/bash.exe"),
        file("C:/Program Files/Git/usr/bin/bash.exe"),
    )

    return candidates.firstOrNull { it.exists() }?.absolutePath
        ?: error("Git Bash was not found. Expected one of: ${candidates.joinToString { it.absolutePath }}")
}

afterEvaluate {
    val nativeTarget = ratexDesktopNative.nativeTarget.orNull
        ?: error("ratexDesktopNative.nativeTarget must be configured for $path")
    val nativeFileName = ratexDesktopNative.nativeFileName.orNull
        ?: error("ratexDesktopNative.nativeFileName must be configured for $path")
    val artifactId = ratexDesktopNative.artifactId.orNull
        ?: error("ratexDesktopNative.artifactId must be configured for $path")
    val supportedHostOs = ratexDesktopNative.supportedHostOs.get()
    val isSupportedHost = currentHostOs() in supportedHostOs
    val nativeFile = rootProject.file("library/native/$nativeTarget/$nativeFileName")

    val commonPom = Action<MavenPom> {
        name.set("RaTeX Desktop Native ($nativeTarget)")
        description.set("Compose Multiplatform LaTeX math rendering powered by RaTeX ($nativeTarget native library)")
        url.set("https://github.com/darriousliu/RaTeX-CMP")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                name.set("RaTeX Contributors")
                url.set("https://github.com/darriousliu/RaTeX-CMP")
            }
        }
        scm {
            url.set("https://github.com/darriousliu/RaTeX-CMP.git")
            connection.set("scm:git:git://github.com/darriousliu/RaTeX-CMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/darriousliu/RaTeX-CMP.git")
        }
    }

    val prepareNativeArtifact = tasks.register<Exec>("prepareNativeArtifact") {
        group = "build"
        description = "Builds the $nativeTarget desktop native artifact."
        onlyIf { isSupportedHost }
        workingDir(rootProject.rootDir)
        commandLine(bashExecutable(), "./prepare-jvm-rust.sh", nativeTarget)
    }

    val verifyNativeArtifact = tasks.register("verifyNativeArtifact") {
        group = "verification"
        description = "Verifies the $nativeTarget desktop native artifact."
        onlyIf { isSupportedHost }
        dependsOn(prepareNativeArtifact)
        doLast {
            check(nativeFile.exists()) {
                "Missing desktop native library: ${nativeFile.absolutePath}. Run prepare-jvm-rust.sh $nativeTarget before building/publishing."
            }
            check(nativeFile.length() > 0L) {
                "Desktop native library is empty: ${nativeFile.absolutePath}. Rebuild with prepare-jvm-rust.sh $nativeTarget before building/publishing."
            }
        }
    }

    tasks.named<ProcessResources>("processResources") {
        dependsOn(verifyNativeArtifact)
        from(nativeFile) {
            into("META-INF/ratex/native/$nativeTarget")
        }
    }

    if (!isSupportedHost) {
        tasks.configureEach {
            if (
                name == "prepareNativeArtifact" ||
                name == "verifyNativeArtifact" ||
                name == "processResources" ||
                name == "classes" ||
                name == "jar" ||
                name == "sourcesJar" ||
                name == "javadoc" ||
                name == "javadocJar" ||
                name.startsWith("generateMetadataFileFor") ||
                name.startsWith("generatePomFileFor") ||
                name.startsWith("sign") ||
                name.startsWith("publish")
            ) {
                enabled = false
            }
        }
    }

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral()
        if (!project.hasProperty("skipSigning")) {
            signAllPublications()
        }
        coordinates(project.group.toString(), artifactId, project.version.toString())
        pom(commonPom)
    }
}
