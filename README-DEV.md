# Controlify 2.0.3 开发环境（FST 特供）

本仓库是 `Controlify 2.0.3` 的源码开发环境，包含 **Minecraft 1.20.1 / Forge 47.4.0** 平台适配、
Create: Pneumatic Tacticals（CPT）手柄兼容、辅助瞄准等本地改动。

> ⚠️ **本分支仅为 FST 提供支持**，不是 Controlify 的通用发行版。
> 只针对 FST 整合包 / RiaFst 实例（MC 1.20.1 + Forge 47.4.0）适配与测试，
> 不保证与其它整合包或服务端兼容。需要通用手柄支持请用上游 [Controlify](https://github.com/isXander/Controlify)。

> 具体改了哪些内容见 **[README.md](README.md)**；1.20.1 Forge 上踩过的坑（初始界面被覆盖、
> dev client 因 YACL 重混淆崩溃）及修法见 **[FIXES-1.20.1-FORGE.md](FIXES-1.20.1-FORGE.md)**；
> 本文档只讲环境与构建。

---

## 1. 环境要求

| 项目 | 要求 |
| --- | --- |
| 操作系统 | Windows 10 / 11、Linux、macOS 均可（本项目主要在 **Linux** 上开发验证） |
| JDK | **JDK 21**（必须）。建议 [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) 或 [Azul Zulu 21](https://www.azul.com/downloads/?version=java-21-lts) |
| 磁盘 | 预留 **10 GB** 以上（Gradle 缓存 + Minecraft 反编译产物） |
| 网络 | 首次构建需要访问 Maven 仓库（Mojang / Forge / NeoForge / isxander 等） |
| 其他 | 无需预装 Gradle，仓库内已含 Gradle Wrapper（8.12） |

### 检查 JDK

```bash
java -version
```

必须显示 `21.x`。若不对，设置 `JAVA_HOME` 后再继续。

**Windows**（路径换成你的实际安装路径，设置后需重开终端）：

```bat
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
```

**Linux**：

```bash
export JAVA_HOME=/usr/lib/jvm/zulu21          # 或 temurin-21 等实际路径
export PATH="$JAVA_HOME/bin:$PATH"
```

只想给单次构建指定 JDK，可以不改环境变量，直接前置：

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew chiseledBuildAndCollect
```

---

## 2. 快速开始

以下命令都在**仓库根目录**（含 `gradlew` / `gradlew.bat` 的那一层）执行。

### 2.1 构建 Forge 1.20.1 版本

```bash
# Linux / macOS
./gradlew chiseledBuildAndCollect

# Windows
gradlew.bat chiseledBuildAndCollect
```

首次构建约 5–20 分钟（取决于网络），之后增量构建只需几秒。

### 2.2 构建产物位置

| 产物 | 路径 | 说明 |
| --- | --- | --- |
| 正式 JAR | `build/finalJars/controlify-2.0.3+1.20.1-forge.jar` | 放进 `mods` 使用（需另装 YACL 3.6.6+1.20.1-forge 等前置） |
| 内置依赖 JAR | `build/finalJars/controlify-2.0.3+1.20.1-forge-offline.jar` | 已打包前置依赖，可直接丢进 `mods` 测试 |

### 2.3 启动开发客户端（可选）

```bash
./gradlew runClientActive          # Linux / macOS
gradlew.bat runClientActive        # Windows
```

首次启动会下载资源与原生库（含 Controlify 的 SDL 原生库），耗时较长。

### 2.4 便捷脚本

仓库根目录提供了一批包装脚本，效果等同上面的 Gradle 命令，并会先检查 JDK 21 与构建目标：

| 用途 | Windows | Linux / macOS |
| --- | --- | --- |
| 构建 Forge 1.20.1 | `build-forge-1.20.1.bat`（可双击） | `./build-forge-1.20.1.sh` |
| 启动开发客户端 | `run-client-forge-1.20.1.bat` | `./run-client-forge-1.20.1.sh` |

> Windows 批处理脚本使用 CRLF 行尾；在 Linux/macOS 上请使用 `.sh` 版本。

---

## 3. 目录说明

```
Controlify-2.0.3/
├─ build.gradle.kts            # 构建脚本（Modstitch + Stonecutter 多版本）
├─ stonecutter.gradle.kts      # Stonecutter 多版本配置
├─ gradle.properties           # 版本号、通用依赖版本
├─ gradlew / gradlew.bat       # Gradle Wrapper
├─ build-logic/                # 自定义 Gradle 插件（VersionParser / StonecutterConfigurator）
├─ src/main/java/              # 主源码（改动主要在这里）
├─ src/main/resources/         # 资源、语言文件、默认手柄键位
├─ versions/                   # 各版本/平台的配置；versions/current 指向当前激活目标
├─ dist/                       # 已构建好的 Forge 1.20.1 JAR（可直接测试）
├─ tools/                      # 辅助工具（CPT 震动调参网页、弹药目录等）
└─ docs/                       # 上游文档
```

### 构建目标

`versions/` 只保留两个目标，定义在 `versions/builds.json`：

| 目标 | Minecraft | 平台 | 说明 |
| --- | --- | --- | --- |
| `1.20.1-forge` | 1.20.1 | Forge | FST 现行目标，`versions/current` 默认指向此项目 |
| `1.21-neoforge` | 1.21.1 | NeoForge | 维护范围上限 |

上游其它平台（Fabric，以及 1.20.4 / 1.20.6 / 1.21.3 / 1.21.4 / 1.21.5）已移除，不再构建。

---

## 4. 常见问题

### 4.1 构建报错：无法解析 `me.modmuss50.mod-publish-plugin:0.6.1+`

`stonecutter.gradle.kts` 顶部用的是**动态版本** `0.6.1+`，个别网络/镜像环境解析不到。
把它固定为具体版本即可：

```kotlin
id("me.modmuss50.mod-publish-plugin") version "0.6.1"
```

改完重新运行构建。

### 4.2 已下载过依赖，想离线构建

在命令后加 `--offline`：

```bash
./gradlew chiseledBuildAndCollect --offline
```

### 4.3 只想构建某一个目标版本

用环境变量指定（不需要改 `versions/current`）：

```bash
# Linux / macOS
CI_SINGLE_BUILD=1.20.1-forge ./gradlew chiseledBuildAndCollect
```

```bat
:: Windows
set CI_SINGLE_BUILD=1.20.1-forge
gradlew.bat chiseledBuildAndCollect
```

### 4.4 `versions/current` 是什么

Stonecutter 的「当前激活版本」标记，内容就是 `versions/` 下的目录名。
本仓库已设为 `1.20.1-forge`，因此直接 `./gradlew build` 只构建 Forge 1.20.1；
想换目标直接编辑这个文件即可。

### 4.5 路径包含中文或空格时报错

把仓库放到**纯 ASCII、无空格**的短路径下，例如 `D:\dev\Controlify` 或 `/home/user/dev/Controlify`。
Gradle、Java 编译器与部分 Mixin 工具链对非 ASCII 路径的兼容性不稳定。

### 4.6 关于 `build-logic`

`settings.gradle.kts` 里通过 `includeBuild("build-logic")` 引入，必须与主工程一起保留，不能删。

### 4.7 Linux 上没权限执行脚本

```bash
chmod +x gradlew *.sh
```

---

## 5. dist 中的预构建产物

| 文件 | SHA-256 |
| --- | --- |
| `dist/controlify-2.0.3+1.20.1-forge.jar` | 见 `dist/SHA256SUMS.txt` |

该 JAR 对应源码提交时的状态；重新构建后哈希会变化，属正常现象。

---

## 6. 不要提交的内容

`.gitignore` 已覆盖以下条目，请勿手动加入版本库：

- `.gradle/`、`build/`、`versions/*/build/`（构建缓存，体积大且与本机绑定）
- `run/`、`runserver/`、`runs/`（开发运行时产生的存档与日志）
- `.kotlin/`、`.run/`、`.idea/`（IDE 与 Kotlin 增量编译产物）

---

## 7. 上游信息

- 上游项目：Controlify（isXander），LGPL-3.0-or-later
- 本仓库基于 2.0.3 版本源码 + 针对 FST（RiaFst，MC 1.20.1 / Forge 47.4.0）的适配改动
- 本分支仅为 FST 提供支持，不保证其它整合包/服务端可用；通用需求请转上游
