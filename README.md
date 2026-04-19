# RaTeX-CMP

✨ RaTeX-CMP 是一个面向多端 UI 场景的数学公式渲染项目，基于 Kotlin Multiplatform 与
Compose Multiplatform 构建。核心渲染能力由 [RaTeX](https://github.com/erweixin/RaTeX) 提供。

让同一套公式渲染能力可以在 Android、iOS 和 JVM Desktop 上复用，方便在Compose
Multiplatform跨平台应用里统一接入数学排版与展示能力。

这个仓库以独立项目的方式维护，既适合作为库继续演进，也适合作为示例工程和集成参考来使用。

## 🌍 支持平台

- Android
- iOS
- JVM Desktop

## 📷 平台截图

下面预留了各平台截图的位置，后续补充截图时可以统一放在 `docs/screenshots/` 目录下。

### Android

预留位置：`docs/screenshots/android.png`

### iOS

预留位置：`docs/screenshots/ios.png`

### JVM Desktop

预留位置：`docs/screenshots/desktop.png`

## 🧭 仓库概览

- `library`：核心库模块
- `example`：共享示例模块，包含 Desktop 运行入口
- `androidApp`：Android 示例应用
- `iosApp`：iOS 示例工程
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
- 执行 `bash prepare-jvm-rust.sh --all` 时，脚本会根据当前机器能力自动选择可构建目标，并自动执行 `rustup target add`
- 例如在 `arm64 macOS` 上，会构建 `darwin-aarch64`、`darwin-x86-64`、`linux-aarch64`、`linux-x86-64`，不会尝试构建 `windows-x86-64`

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

发布全部 `library` 产物（包含 KMP 主库、各平台产物以及 JVM Desktop native 库）：

```bash
./gradlew :library:publishAndReleaseToMavenCentral
```

单独发布 KMP 主库：

```bash
./gradlew :library:publishKotlinMultiplatformPublicationToMavenCentralRepository
```

发布当前机器支持的全部 JVM Desktop native 库：

```bash
./gradlew :library:publishSupportedDesktopNativePublicationsToMavenCentralRepository
```

发布当前机器支持的全部 JVM Desktop native 库到 Maven Local：

```bash
./gradlew :library:publishSupportedDesktopNativePublicationsToMavenLocal
```

这个任务会自动执行：

- `bash prepare-jvm-rust.sh --all`
- 校验当前机器支持的 Desktop native 产物是否生成成功
- 发布当前机器支持的所有 Desktop native publication

例如：

- 在 `arm64 macOS` 上，会发布 `darwin-aarch64`、`darwin-x86-64`、`linux-aarch64`、`linux-x86-64`
- 在 `Linux` 上，会发布 `linux-aarch64`、`linux-x86-64`
- 在 `Windows` 上，会发布 `windows-x86-64`

## 🙏 致谢

感谢 [RaTeX](https://github.com/erweixin/RaTeX) 项目提供的核心能力与开源基础。

也感谢 Kotlin Multiplatform、Compose Multiplatform、Rust 以及相关开源社区，让这个跨平台项目能够持续演进，并以更统一的方式服务多端应用开发。
