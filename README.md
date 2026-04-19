# RaTeX-CMP

[English Version](README-en.md) | [中文版本](README.md)

[![许可证](https://img.shields.io/badge/License-MIT-orange.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)]([http://kotlinlang.org](https://www.jetbrains.com/kotlin-multiplatform/))

✨ RaTeX-CMP 是一个面向多端 UI 场景的数学公式渲染项目，基于 Kotlin Multiplatform 与
Compose Multiplatform 构建。核心渲染能力由 [RaTeX](https://github.com/erweixin/RaTeX) 提供。

让同一套公式渲染能力可以在 Android、iOS 和 JVM Desktop 上复用，方便在Compose
Multiplatform跨平台应用里统一接入数学排版与展示能力。

这个仓库以独立项目的方式维护，既适合作为库继续演进，也适合作为示例工程和集成参考来使用。

## 🌍 支持平台

| 平台          | 架构 / 目标                                                              | 备注                                   |
|-------------|----------------------------------------------------------------------|--------------------------------------|
| Android     | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`                          | `x86` 目前未测试                          |
| iOS         | iPhone / Simulator                                                   | 通过 Kotlin Multiplatform Framework 集成 |
| JVM Desktop | Windows `x86_64`, macOS `x86_64` / `arm64`, Linux `x86_64` / `arm64` | Desktop native 库按当前机器支持的平台构建与发布      |

## 📷 平台截图

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

## 🚀 使用方法

### 1. 添加仓库

如果你从 Maven Central 使用，确认项目仓库中已包含：

```kotlin
repositories {
    mavenCentral()
}
```

如果你是先本地验证再接入，也可以使用：

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

### 2. 引入依赖

当前 KMP 主库坐标为：

```kotlin
implementation("io.github.darriousliu:ratex:0.1.2")
```

在 Kotlin Multiplatform 项目中，通常添加到 `commonMain`：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.darriousliu:ratex:0.1.2")
        }
    }
}
```

如果你要在 JVM Desktop 上运行，还需要额外添加当前平台对应的 native 运行时依赖：

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

可选的 Desktop native 坐标有：

- `io.github.darriousliu:ratex-native-darwin-aarch64`
- `io.github.darriousliu:ratex-native-darwin-x86-64`
- `io.github.darriousliu:ratex-native-linux-aarch64`
- `io.github.darriousliu:ratex-native-linux-x86-64`
- `io.github.darriousliu:ratex-native-windows-x86-64`

在这个仓库里，Desktop native 库本身是独立发布的子模块；示例工程会按当前主机平台自动选择对应的运行时依赖。

### 3. 使用 Compose 组件

最简单的用法是直接传入 LaTeX 字符串：

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

如果你想渲染行内公式，可以将 `displayMode` 设为 `false`：

```kotlin
RaTeX(
    latex = """e^{i\pi}+1=0""",
    fontSize = 20.sp,
    displayMode = false,
)
```

### 4. 复用解析结果

如果你希望先解析，再在多个地方复用 `DisplayList`，可以使用 `rememberRaTeXDisplayList`：

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

### 5. 主要参数说明

- `latex`：要渲染的 LaTeX 公式字符串
- `fontSize`：公式渲染字号
- `displayMode`：`true` 为块级公式，`false` 为行内公式
- `displayList`：已解析好的绘制结果，适合缓存或复用

## 🧭 仓库概览

- `library`：核心库模块
- `desktop-native/*`：JVM Desktop native 库发布模块
- `example`：共享示例模块，包含 Desktop 运行入口
- `androidApp`：Android 示例应用
- `iosApp`：iOS 示例工程
- `build-logic`：共享 Gradle 约定插件，封装 Desktop native 发布逻辑
- `external/RaTeX`：上游 RaTeX submodule

## 🛠️ 本地开发

### 1. 克隆仓库并初始化 submodule

```bash
git clone https://github.com/darriousliu/RaTeX-CMP.git
cd RaTeX-CMP
git submodule update --init --recursive
```

如果已经克隆过仓库但没有初始化 submodule，也可以只执行最后一行命令。

### 2. 准备基础环境

建议准备以下工具：

- JDK 17
- Android Studio 或 IntelliJ IDEA
- Rust toolchain
- Bash 环境
- Android SDK；如果要构建 Android 原生库，还需要 Android NDK
- Xcode；仅在 macOS 上开发 iOS 时需要

按平台不同，可能还会用到这些工具：

- Android：`cargo-ndk`
- Desktop 全平台 native 打包：`cargo-zigbuild` 和 `zig`

### 3. 安装对应平台的 Rust target

在首次运行或构建某个平台之前，建议先安装该平台所需的 Rust target。

Android：

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
```

iOS：

```bash
rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios
```

JVM Desktop：

- 构建当前主机平台时，通常不需要额外执行 `rustup target add`
- 执行 `bash prepare-jvm-rust.sh --all` 时，脚本会根据当前机器能力自动选择可构建目标，并自动执行
  `rustup target add`
- 例如在 `arm64 macOS` 上，会构建 `darwin-aarch64`、`darwin-x86-64`、`linux-aarch64`、`linux-x86-64`
  ，不会尝试构建 `windows-x86-64`

### 4. 准备本地产物

如果你第一次运行项目，或者修改了底层 Rust 代码，通常需要先准备对应平台的本地产物。

Android：

```bash
bash prepare-android-rust.sh
```

iOS：

```bash
bash prepare-ios-rust.sh
```

JVM Desktop：

```bash
bash prepare-jvm-rust.sh
```

准备当前机器可构建的全部 Desktop Rust 产物：

```bash
bash prepare-jvm-rust.sh --all
```

如果你只是做 Kotlin / Compose 层开发，并且仓库里已经有可用产物，可以按需执行，而不必每次都重复准备。

### 5. 运行与验证

JVM Desktop 示例：

```bash
./gradlew :example:run
```

Windows 下可以使用：

```bash
.\gradlew.bat :example:run
```

Android Debug 包：

```bash
./gradlew :androidApp:assembleDebug
```

Windows 下可以使用：

```bash
.\gradlew.bat :androidApp:assembleDebug
```

iOS 开发建议在 macOS 上打开 `iosApp/iosApp.xcodeproj` 进行调试。

### 6. 开发建议

- 优先在当前仓库内完成 Compose UI、Kotlin API 和示例工程相关开发
- Desktop native 发布相关改动优先收敛到 `build-logic`
- 需要联动底层能力时，再进入 `external/RaTeX`
- 更新 submodule 后，记得重新准备对应平台的本地产物
- 提交改动时，注意区分当前项目改动与 submodule 改动

## 🚀 常用命令

初始化 submodule：

```bash
git submodule update --init --recursive
```

运行 Desktop 示例：

```bash
./gradlew :example:run
```

构建 Android 示例：

```bash
./gradlew :androidApp:assembleDebug
```

准备 Android Rust 产物：

```bash
bash prepare-android-rust.sh
```

准备 iOS Rust 产物：

```bash
bash prepare-ios-rust.sh
```

准备 Desktop Rust 产物：

```bash
bash prepare-jvm-rust.sh
```

准备当前机器可构建的全部 Desktop Rust 产物：

```bash
bash prepare-jvm-rust.sh --all
```

发布库到 Maven Central：

发布前请先准备好发布凭据与签名配置。

发布当前机器支持的全部产物到 Maven Local：

```bash
./gradlew publishToMavenLocal
```

这个命令会发布：

- `:library` 的 KMP 主库
- 当前机器支持的 Desktop native 子模块

这也是当前推荐的本地验证方式；如果命令成功，说明主库和当前机器可发布的 Desktop native 库都会进入本地 Maven 仓库。

例如：

- 在 `arm64 macOS` 上，会额外发布 `ratex-native-darwin-aarch64`、`ratex-native-darwin-x86-64`、`ratex-native-linux-aarch64`、`ratex-native-linux-x86-64`
- 在 `Linux` 上，会额外发布 `ratex-native-linux-aarch64`、`ratex-native-linux-x86-64`
- 在 `Windows` 上，会额外发布 `ratex-native-windows-x86-64`

发布当前机器支持的全部产物到 Maven Central：

```bash
./gradlew publishAndReleaseToMavenCentral
```

单独发布 KMP 主库：

```bash
./gradlew :library:publishKotlinMultiplatformPublicationToMavenCentralRepository
```

发布当前机器支持的全部 JVM Desktop native 库：

```bash
./gradlew publishSupportedDesktopNativePublicationsToMavenCentralRepository
```

发布当前机器支持的全部 JVM Desktop native 库到 Maven Local：

```bash
./gradlew publishSupportedDesktopNativePublicationsToMavenLocal
```

这个任务会自动执行：

- 当前机器支持的 native 子模块发布任务
- 每个子模块内会自动调用对应的 `prepare-jvm-rust.sh <target>`
- 校验当前机器支持的 Desktop native 产物是否生成成功

这些 Desktop native 子模块共享同一套 `build-logic` 预编译脚本插件配置，只在各自模块里声明目标平台、文件名、artifactId 和支持的宿主机范围。

例如：

- 在 `arm64 macOS` 上，会发布 `darwin-aarch64`、`darwin-x86-64`、`linux-aarch64`、`linux-x86-64`
- 在 `Linux` 上，会发布 `linux-aarch64`、`linux-x86-64`
- 在 `Windows` 上，会发布 `windows-x86-64`

## 🙏 致谢

感谢 [RaTeX](https://github.com/erweixin/RaTeX) 项目提供的核心能力与开源基础。

也感谢 Kotlin Multiplatform、Compose Multiplatform、Rust 以及相关开源社区，让这个跨平台项目能够持续演进，并以更统一的方式服务多端应用开发。
