# RaTeX 0.1.15 升级与验证记录

验证日期：2026-10-16。

## 升级范围

- Rust 子模块固定到上游 `v0.1.15`（`5f7dc4883aba8eeb30fe34f8a104923f43f734bf`）。
- CMP 发布版本、Web 的 `ratex-wasm` 依赖及 JS/Wasm 两份 Yarn 锁文件统一为 `0.1.15`。
- iOS 使用上游固定的 `nightly-2026-08-16` 和 `-Z build-std=std,panic_abort`；复制静态库前检查 unwind 重定位。
- Android 构建四个 ABI，并使用上游脚本校验两个 64 位 ABI 的 ELF LOAD/GNU_RELRO 16 KB 对齐；发布流水线再次检查打包后的 AAR。
- 发布前增加 JVM 和 iOS 模拟器回归测试。
- 修复测试基础设施：JS UI 测试等待 Skiko 就绪；Android 自包含测试 APK 引入 Compose 测试 Activity。

现有 CMP 解析 API 和 DisplayList 解码协议保持兼容。上游新增的回退字形度量接口目前只在 Rust 布局层可用，本次升级没有接入该接口。

## 新增回归覆盖

共享测试套件增加 6 项测试，覆盖：

1. `\slashed` 的叠加、宽度，以及带花括号和不带花括号参数的一致性。
2. `\centernot` 的居中位置与关系符间距。
3. GlyphPath、Line、Rect、Path 对公式颜色的继承。
4. 宏展开资源超限及错误后恢复。
5. 布局资源超限及错误后恢复。
6. 十进制、十六进制、八进制 `\char` 溢出拒绝及错误后恢复。

## 实际验证结果

| 平台 | 结果 | 环境 / 方式 |
| --- | --- | --- |
| JVM | 39 项通过 | macOS arm64，JDK 21 |
| macOS Native | 37 项通过 | `macosArm64Test` |
| iOS Simulator | 36 项通过 | arm64，iOS 27 模拟器 |
| JS Browser | 35 项通过 | Chrome，`jsBrowserTest` |
| Wasm Browser | 35 项通过 | Chrome，`wasmJsBrowserTest` |
| Android | 36 项通过 | Android 11 / API 30，完整 instrumentation 测试 |
| Android 16 KB 页复验 | 34 项引擎测试通过 | Android 17 / API 37，实际页大小 16384 |

前六个平台合计 218 项通过；Android 16 KB 环境另外复验同一套引擎测试。六个平台均实际执行了上述 6 项新增回归，浏览器还使用过临时异步失败探针，确认协程中的失败能使测试失败；探针已移除。

构建与产物检查：

- `prepare-ios-rust.sh`：设备 arm64、模拟器 arm64 静态库均构建成功，无 `_rust_eh_personality` unwind 重定位。
- `prepare-android-rust.sh`：`arm64-v8a`、`armeabi-v7a`、`x86_64`、`x86` 均构建成功；两个 64 位 ABI 的 LOAD/GNU_RELRO 对齐通过。
- `prepare-jvm-rust.sh darwin-aarch64`：构建成功。
- `:library:linkReleaseFrameworkIosArm64`：Release framework 链接任务成功。
- `:library:bundleAndroidMainAar`：构建成功；`library/build/outputs/aar/library.aar` 内两个 64 位 ABI 校验通过。
- `bash -n`、`git diff --check`：通过。

## 复现命令与环境限制

```bash
bash prepare-ios-rust.sh
bash prepare-android-rust.sh
bash prepare-jvm-rust.sh darwin-aarch64

./gradlew :library:jvmTest :library:macosArm64Test :library:iosSimulatorArm64Test
./gradlew :library:jsBrowserTest :library:wasmJsBrowserTest
./gradlew :library:linkReleaseFrameworkIosArm64 :library:bundleAndroidMainAar
python3 external/RaTeX/scripts/verify_android_elf.py library/build/outputs/aar/library.aar

# 正常情况下由 Gradle 驱动已连接设备上的测试
./gradlew :library:connectedAndroidDeviceTest
```

本机 Gradle 的 `connectedAndroidDeviceTest` 被 Google Maven 的 UTP 插件 JAR 下载 TLS 错误阻断，因此实际使用相同测试 APK 和 AndroidJUnitRunner 直接执行，并依据 `OK (36 tests)` 判定结果：

```bash
./gradlew :library:assembleAndroidDeviceTest
adb -s <serial> install -r library/build/outputs/apk/androidTest/library-androidTest.apk
adb -s <serial> shell am instrument -w -r \
  io.ratex.compose.test/androidx.test.runner.AndroidJUnitRunner

# 在 16 KB 页设备上单独复验引擎
adb -s <serial> shell getconf PAGE_SIZE
adb -s <serial> shell am instrument -w -r \
  -e class io.ratex.RaTeXEngineAndroidDeviceTest \
  io.ratex.compose.test/androidx.test.runner.AndroidJUnitRunner
```

现有 Espresso 在 Android 17 上访问 `InputManager.getInstance` 失败，所以完整 UI 测试在 Android 11 上验证；未通过变更生产依赖规避这一测试框架兼容问题。尝试的临时 Android 16 / 16 KB 模拟器在启动阶段崩溃，未计入通过结果。临时模拟器已关闭，用户原有设备保持运行。

本次验证不包含真机签名安装、远端发布流水线执行或最终应用 APK 的 ZIP 对齐校验。ELF 对齐检查针对库本身，不能替代应用打包后的 ZIP 对齐检查。
