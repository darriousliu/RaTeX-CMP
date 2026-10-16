# 显式回退字形度量桥接

CMP 通过自有 `rust-bridge` 调用 RaTeX 0.1.15 的 `layout_with_fallback_metrics`。
调用方按 Unicode 码点提供度量；接口不测量、不加载或绑定字体，原有解析和绘制接口保持不变。

```kotlin
val metrics = RaTeXFallbackMetrics.of(mapOf(
    '中'.code to FallbackGlyphMetrics(widthEm = 1.15, heightEm = 0.8, depthEm = 0.12),
    '文'.code to FallbackGlyphMetrics(widthEm = 1.15, heightEm = 0.8, depthEm = 0.12),
))
val displayList = RaTeXEngine.parseWithFallbackMetrics("""\text{中文}""", metrics)
// 使用原有 RaTeX(displayList = displayList) 绘制。
```

以上为演示值。实际度量必须与绘制字体一致，以基础字号归一到 em：
`widthEm = advancePx / fontSizePx`，`heightEm = max(0, -inkTop) / fontSizePx`，
`depthEm = max(0, inkBottom) / fontSizePx`。不要预乘上下标或 `\Huge` 缩放。

- 只覆盖核心缺少度量的字形；已有度量和未指定字符保持原行为。
- 空表直接调用原解析入口。输入 Map 会被复制，每次调用独立生效。
- 键支持非 BMP Unicode scalar；最多 4096 项，各维度须为 0–100 em 内有限数。
- 同步入口为 `parseBlockingWithFallbackMetrics`。浏览器首次同步使用非空表前须挂起调用
  `prepareFallbackMetrics()`；异步入口会自动初始化桥接。
- 仍按单个码点处理，不新增 shaping、连字或组合字符簇能力。

## 构建

上游 submodule 和官方 npm 版本保持不变。Android/JVM/Apple 构建脚本额外构建并打包
`ratex_cmp_bridge`；Web 通过 `prepareBridgeWasm` 生成 Compose 资源，首次使用新接口时加载。
桥接包含一份解析与布局代码，会增加包体积。

```bash
bash prepare-jvm-rust.sh darwin-aarch64
bash prepare-ios-rust.sh
bash prepare-android-rust.sh
cargo fmt --manifest-path rust-bridge/Cargo.toml -- --check
cargo test --manifest-path rust-bridge/Cargo.toml --locked
./gradlew :library:jvmTest :library:macosArm64Test :library:iosSimulatorArm64Test
./gradlew :library:jsBrowserTest :library:wasmJsBrowserTest
./gradlew :library:connectedAndroidDeviceTest
node rust-bridge/tests/wasm-smoke.mjs library/build/generated/bridgeWebResources/files/bridge/ratex_cmp_bridge.wasm
```

浏览器测试需要 Chrome；必要时设置 `CHROME_BIN`。源码构建需要 Rust；已发布包的使用方无需安装。
example 的 **0.1.15 additions** 分类中仅新增 **CMP explicit fallback metrics** 条目，
对比默认布局和手动传入的度量。回归覆盖空表兼容、缺失字形、非 BMP、缩放、调用隔离和失败恢复。
