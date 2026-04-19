# RaTeX-CMP

[English Version](README-en.md) | [中文版本](README.md)

[![License](https://img.shields.io/badge/License-MIT-orange.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)]([http://kotlinlang.org](https://www.jetbrains.com/kotlin-multiplatform/))

✨ RaTeX-CMP is a math formula rendering project for multi-platform UI scenarios, built with Kotlin Multiplatform and Compose Multiplatform. Its core rendering capability is powered by [RaTeX](https://github.com/erweixin/RaTeX).

It allows the same formula rendering engine to be reused across Android, iOS, and JVM Desktop, making it easier to integrate consistent mathematical typesetting and display into Compose Multiplatform applications.

This repository is maintained as an independent project. It can continue evolving as a library while also serving as a sample project and integration reference.

## 🌍 Supported Platforms

| Platform | Architectures / Targets | Notes |
| --- | --- | --- |
| Android | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` | `x86` is currently untested |
| iOS | iPhone / Simulator | Integrated via Kotlin Multiplatform Framework |
| JVM Desktop | Windows `x86_64`, macOS `x86_64` / `arm64`, Linux `x86_64` / `arm64` | Desktop native libraries are built and published based on what the current machine supports |

## 📷 Screenshots

<table>
  <thead>
    <tr>
      <th width="25%">Android</th>
      <th width="25%">iOS</th>
      <th width="50%">JVM Desktop</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><img alt="android.png" src="docs/screenshots/android.png" width="100%"/></td>
      <td><img alt="ios.png" src="docs/screenshots/ios.png" width="100%"/></td>
      <td><img alt="desktop.png" src="docs/screenshots/desktop.png" width="100%"/></td>
    </tr>
  </tbody>
</table>

## 🚀 Usage

### 1. Add repositories

If you are using Maven Central, make sure your project repositories include:

```kotlin
repositories {
    mavenCentral()
}
```

If you want to validate locally first, you can also use:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

### 2. Add the dependency

The current KMP main library coordinates are:

```kotlin
implementation("io.github.darriousliu:ratex:0.1.2")
```

In a Kotlin Multiplatform project, you would typically add it to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.darriousliu:ratex:0.1.2")
        }
    }
}
```

If you want to run on JVM Desktop, you also need to add the native runtime dependency for the current platform:

```kotlin
kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation("io.github.darriousliu:ratex:0.1.2")
            runtimeOnly("io.github.darriousliu:ratex-native-darwin-aarch64:0.1.2")
        }
    }
}
```

Available Desktop native coordinates:

- `io.github.darriousliu:ratex-native-darwin-aarch64`
- `io.github.darriousliu:ratex-native-darwin-x86-64`
- `io.github.darriousliu:ratex-native-linux-aarch64`
- `io.github.darriousliu:ratex-native-linux-x86-64`
- `io.github.darriousliu:ratex-native-windows-x86-64`

In this repository, Desktop native libraries are published as separate submodules; the sample app automatically selects the matching runtime dependency for the current host platform.

### 3. Use the Compose component

The simplest usage is to pass a LaTeX string directly:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import io.ratex.compose.RaTeX

@Composable
fun FormulaSample(modifier: Modifier = Modifier) {
    RaTeX(
        latex = """\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}""",
        modifier = modifier,
        fontSize = 28.sp,
        displayMode = true,
    )
}
```

If you want inline math, set `displayMode = false`:

```kotlin
RaTeX(
    latex = """e^{i\pi}+1=0""",
    fontSize = 20.sp,
    displayMode = false,
)
```

### 4. Reuse parsed results

If you want to parse first and reuse the resulting `DisplayList` in multiple places, you can use `rememberRaTeXDisplayList`:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import io.ratex.compose.RaTeX
import io.ratex.compose.rememberRaTeXDisplayList

@Composable
fun ParsedFormulaSample(latex: String) {
    val parseResult by rememberRaTeXDisplayList(
        latex = latex,
        displayMode = true,
    )

    RaTeX(
        displayList = parseResult?.getOrNull(),
        fontSize = 28.sp,
    )
}
```

### 5. Main parameters

- `latex`: the LaTeX formula string to render
- `fontSize`: the rendering font size
- `displayMode`: `true` for block math, `false` for inline math
- `displayList`: a parsed drawing result that is useful for caching or reuse

## 🧭 Repository Overview

- `library`: core library module
- `desktop-native/*`: publishing modules for JVM Desktop native libraries
- `example`: shared sample module, including the Desktop entry point
- `androidApp`: Android sample app
- `iosApp`: iOS sample project
- `build-logic`: shared Gradle convention plugins for Desktop native publishing
- `external/RaTeX`: upstream RaTeX submodule

## 🛠️ Local Development

### 1. Clone the repository and initialize submodules

```bash
git clone https://github.com/darriousliu/RaTeX-CMP.git
cd RaTeX-CMP
git submodule update --init --recursive
```

If you have already cloned the repository but have not initialized submodules, you can simply run the last command.

### 2. Prepare the base environment

Recommended tools:

- JDK 17
- Android Studio or IntelliJ IDEA
- Rust toolchain
- Bash environment
- Android SDK; Android NDK is also required if you want to build Android native libraries
- Xcode; required only when developing for iOS on macOS

Depending on the platform, you may also need:

- Android: `cargo-ndk`
- Desktop full-platform native packaging: `cargo-zigbuild` and `zig`

### 3. Install the required Rust targets

Before running or building for a platform for the first time, it is recommended to install the required Rust targets for that platform.

Android:

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
```

iOS:

```bash
rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios
```

JVM Desktop:

- When building only for the current host platform, you usually do not need to run `rustup target add` manually
- When running `bash prepare-jvm-rust.sh --all`, the script automatically selects all targets supported by the current machine and runs `rustup target add` as needed
- For example, on `arm64 macOS`, it builds `darwin-aarch64`, `darwin-x86-64`, `linux-aarch64`, and `linux-x86-64`, but does not attempt `windows-x86-64`

### 4. Prepare local artifacts

If this is your first time running the project, or if you changed the underlying Rust code, you will usually need to prepare the corresponding local artifacts first.

Android:

```bash
bash prepare-android-rust.sh
```

iOS:

```bash
bash prepare-ios-rust.sh
```

JVM Desktop:

```bash
bash prepare-jvm-rust.sh
```

Prepare all Desktop Rust artifacts supported by the current machine:

```bash
bash prepare-jvm-rust.sh --all
```

If you are only working on the Kotlin / Compose layer and usable artifacts already exist in the repository, run these commands only when needed instead of every time.

### 5. Run and verify

JVM Desktop sample:

```bash
./gradlew :example:run
```

On Windows:

```bash
.\gradlew.bat :example:run
```

Android debug package:

```bash
./gradlew :androidApp:assembleDebug
```

On Windows:

```bash
.\gradlew.bat :androidApp:assembleDebug
```

For iOS development, it is recommended to open `iosApp/iosApp.xcodeproj` on macOS for debugging.

### 6. Development suggestions

- Prefer completing Compose UI, Kotlin API, and sample project work within this repository first
- Prefer keeping Desktop native publishing changes inside `build-logic`
- Enter `external/RaTeX` only when you need to coordinate with the lower-level engine
- After updating the submodule, remember to regenerate the corresponding platform artifacts
- When committing changes, distinguish carefully between changes in this project and changes in the submodule

## 🚀 Common Commands

Initialize submodules:

```bash
git submodule update --init --recursive
```

Run the Desktop sample:

```bash
./gradlew :example:run
```

Build the Android sample:

```bash
./gradlew :androidApp:assembleDebug
```

Prepare Android Rust artifacts:

```bash
bash prepare-android-rust.sh
```

Prepare iOS Rust artifacts:

```bash
bash prepare-ios-rust.sh
```

Prepare Desktop Rust artifacts:

```bash
bash prepare-jvm-rust.sh
```

Prepare all Desktop Rust artifacts supported by the current machine:

```bash
bash prepare-jvm-rust.sh --all
```

Publish the library to Maven Central:

Make sure your publishing credentials and signing configuration are ready before publishing.

Publish all artifacts supported by the current machine to Maven Local:

```bash
./gradlew publishToMavenLocal
```

This publishes:

- the KMP main library from `:library`
- Desktop native submodules supported by the current machine

This is also the recommended local verification path. If it succeeds, both the main library and the Desktop native libraries supported by the current machine will be published to your local Maven repository.

For example:

- On `arm64 macOS`, it also publishes `ratex-native-darwin-aarch64`, `ratex-native-darwin-x86-64`, `ratex-native-linux-aarch64`, and `ratex-native-linux-x86-64`
- On `Linux`, it also publishes `ratex-native-linux-aarch64` and `ratex-native-linux-x86-64`
- On `Windows`, it also publishes `ratex-native-windows-x86-64`

Publish all artifacts supported by the current machine to Maven Central:

```bash
./gradlew publishAndReleaseToMavenCentral
```

Publish only the KMP main library:

```bash
./gradlew :library:publishKotlinMultiplatformPublicationToMavenCentralRepository
```

Publish all JVM Desktop native libraries supported by the current machine:

```bash
./gradlew publishSupportedDesktopNativePublicationsToMavenCentralRepository
```

Publish all JVM Desktop native libraries supported by the current machine to Maven Local:

```bash
./gradlew publishSupportedDesktopNativePublicationsToMavenLocal
```

This task automatically:

- runs the publish tasks for native submodules supported by the current machine
- automatically calls the matching `prepare-jvm-rust.sh <target>` inside each submodule
- verifies that Desktop native artifacts supported by the current machine were generated successfully

These Desktop native submodules share the same precompiled script plugin from `build-logic`; each module only declares its target platform, file name, artifactId, and supported host OS set.

For example:

- On `arm64 macOS`, it publishes `darwin-aarch64`, `darwin-x86-64`, `linux-aarch64`, and `linux-x86-64`
- On `Linux`, it publishes `linux-aarch64` and `linux-x86-64`
- On `Windows`, it publishes `windows-x86-64`

## 🙏 Acknowledgements

Thanks to the [RaTeX](https://github.com/erweixin/RaTeX) project for providing the core capabilities and open-source foundation.

Thanks also to Kotlin Multiplatform, Compose Multiplatform, Rust, and the related open-source communities for making this cross-platform project possible and helping it continue to evolve in a more unified way across platforms.
