# Controlify 2.0.3 开发环境（FST 特供 · Windows）

本包是 `Controlify 2.0.3` 的源码开发环境快照，包含 **Minecraft 1.20.1 / Forge 47.4.0** 平台适配、
Create: Pneumatic Tacticals（CPT）手柄兼容、辅助瞄准等本地改动。

> **本分支仅为 FST 提供支持**，不是 Controlify 的通用发行版。
> 只针对 FST 整合包 / RiaFst 实例（MC 1.20.1 + Forge 47.4.0）适配与测试，
> 不保证与其它整合包或服务端兼容。需要通用手柄支持请用上游 [Controlify](https://github.com/isXander/Controlify)。

> 本包**只含源码与预构建产物**，不含 Gradle 缓存 / 构建输出（原始目录约 911 MB，压缩后约 7 MB）。
> 第一次构建会自行下载依赖，请保证网络可用。

---

## 1. 环境要求

| 项目 | 要求 |
| --- | --- |
| 操作系统 | Windows 10 / 11（64 位） |
| JDK | **JDK 21**（必须）。建议 [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) 或 [Azul Zulu 21](https://www.azul.com/downloads/?version=java-21-lts) |
| 磁盘 | 预留 **10 GB** 以上（Gradle 缓存 + Minecraft 反编译产物） |
| 网络 | 首次构建需要访问 Maven 仓库（Mojang / Forge / Fabric / isxander 等） |
| 其他 | 无需预装 Gradle，包内已含 Gradle Wrapper（8.12） |

### 检查 JDK

打开 PowerShell（或 CMD）：

```bat
java -version
```

必须显示 `21.x`。若版本不对，设置一次环境变量（把路径换成你的实际安装路径）：

```bat
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
```

设置后**重新打开**一个终端再继续。

---

## 2. 快速开始

在包根目录（含 `gradlew.bat` 的那一层）打开 PowerShell：

### 2.1 构建 Forge 1.20.1 版本

```bat
.\gradlew.bat chiseledBuildAndCollect
```

也可以直接双击包内的 **`build-forge-1.20.1.bat`**。

首次构建约 5–20 分钟（取决于网络），之后增量构建只需几秒。

### 2.2 构建产物位置

| 产物 | 路径 | 说明 |
| --- | --- | --- |
| 正式 JAR | `build\finalJars\controlify-2.0.3+1.20.1-forge.jar` | 放进 `mods` 使用（需另装 YACL 3.6.6+1.20.1-forge 等前置） |
| 内置依赖 JAR | `build\finalJars\controlify-2.0.3+1.20.1-forge-offline.jar` | 已打包前置依赖，可直接丢进 `mods` 测试 |

### 2.3 启动开发客户端（可选）

```bat
.\gradlew.bat runClientActive
```

首次启动会下载资源与原生库（含 Controlify 的 SDL 原生库），耗时较长。

---

## 3. 目录说明

```
Controlify-2.0.3-dev\
├─ build.gradle.kts            # 构建脚本（Modstitch + Stonecutter 多版本）
├─ stonecutter.gradle.kts      # Stonecutter 多版本配置
├─ gradle.properties           # 版本号、各平台依赖版本
├─ gradlew.bat / gradlew       # Gradle Wrapper
├─ build-logic\                # 自定义 Gradle 插件（VersionParser / StonecutterConfigurator）
├─ src\main\java\              # 主源码（本次改动主要在这里）
├─ src\main\resources\         # 资源、语言文件、默认手柄键位
├─ versions\                   # 各版本/平台的配置；versions\current 指向当前激活目标
├─ dist\                       # 已构建好的 Forge 1.20.1 JAR（可直接测试）
├─ tools\                      # 辅助工具（CPT 震动调参网页、弹药目录等）
└─ docs\                       # 上游文档
```

---

## 4. 本次改动摘要

### 4.1 Forge 1.20.1 平台支持

- 新增 Forge 平台适配层：`src\main\java\dev\isxander\controlify\platform\forge\`
  与 `platform\client\forge\`、`platform\main\forge\`、`platform\network\forge\`。
- 入口与 Mixin 配置：`ControlifyBootstrap.java`、`controlify-platform.forge.mixins.json`。
- Forge 资源监听器在 mod 构造阶段注册（避免错过首次资源重载）。
- Forge 使用 JAR 清单注册 Mixin 并提供 refmap；回调使用 SRG 方法目标。
- 服务器可不装 Controlify：客户端 mod 不强制服务端存在对应 channel。

### 4.2 CPT（Create: Pneumatic Tacticals）兼容

- 兼容包路径：`src\main\java\dev\isxander\controlify\compatibility\cpt\`
- `CptReflection.java` 通过**反射**访问 CPT 公开 API，避免把 CPT 普通类当作 Mixin 接口目标而崩溃。
- 仅在**主手持有 CPT 枪械**时启用枪械相关逻辑。
- 右摇杆选弹、视角抑制、按弹药伤害缩放的马达震动（含左右扳机马达）。
  震动配置：`config\controlify\cpt-rumble.json`；调参工具：`tools\cpt-rumble-tuner.html`。
- 持枪时，**手动改绑的 CPT 按键优先于冲突的 Controlify 默认绑定**；
  未持枪时保留原版按键行为。
  相关逻辑：`bindings\InputBindingImpl.java` 的 `isOverriddenByManualBinding(...)`。

### 4.3 CPT 默认手柄键位预设（已内置到 mod）

文件：`src\main\resources\assets\controlify\controllers\default_bind\default.json`

| CPT 功能 | 手柄按键 |
| --- | --- |
| 换弹 `reload` | `button/west`（X / □） |
| 射击模式 `fire_mode` | `dpad_right` |
| 切换弹药 `cycle_ammo` | `dpad_up` |

> CPT 的 `aim_stance`、`interact` 未分配预设，保持未绑定。
> 已有玩家若手动改过绑，默认值不会覆盖其配置。

### 4.4 辅助瞄准（Aim Assist）

- 实现：`src\main\java\dev\isxander\controlify\ingame\AimAssist.java`
  通过 `ControlifyEvents.LOOK_INPUT_MODIFIER` 修改视角输入。
- 触发：**按住瞄准输入**（弓/弩/三叉戟/望远镜的使用状态，或 CPT Aim Stance / LT）时生效，
  右摇杆居中也持续吸附。
- 默认/上限：强度 **60%**、视野角度 **30°**、距离 **128 格**。
- 目标筛选：视野内、无遮挡、`isPickable` 的存活生物（非旁观者）。
- 配置项在「手柄设置 → 灵敏度」分组中，可随时关闭。

---

## 5. 常见问题

### 5.1 构建报错：无法解析 `me.modmuss50.mod-publish-plugin:0.6.1+`

`stonecutter.gradle.kts` 顶部用的是**动态版本** `0.6.1+`，个别网络/镜像环境解析不到。
把它固定为具体版本即可：

```kotlin
id("me.modmuss50.mod-publish-plugin") version "0.6.1"
```

改完重新运行构建。

### 5.2 已下载过依赖，想离线构建

在命令后加 `--offline`：

```bat
.\gradlew.bat chiseledBuildAndCollect --offline
```

### 5.3 只想构建某一个目标版本

用环境变量指定（不需要改 `versions\current`）：

```bat
set CI_SINGLE_BUILD=1.20.1-forge
.\gradlew.bat chiseledBuildAndCollect
```

其他可用目标见 `versions\` 目录名（如 `1.20.1-fabric`、`1.21.5-neoforge` 等）。

### 5.4 `versions\current` 是什么

Stonecutter 的「当前激活版本」标记。本包已设为 `1.20.1-forge`，
因此直接 `.\gradlew.bat build` 只构建 Forge 1.20.1。
切换版本也可以直接编辑这个文件（内容就是目录名）。

### 5.5 中文路径 / 空格路径报错

把包解压到**纯英文、无空格**的短路径下，例如 `D:\dev\Controlify`。
Gradle、Java 编译器与部分 Mixin 工具链对非 ASCII 路径的兼容性不稳定。

### 5.6 关于 `build-logic`

`settings.gradle.kts` 里通过 `includeBuild("build-logic")` 引入，必须与主工程一起保留，不能删。

---

## 6. dist 中的预构建产物

| 文件 | SHA-256 |
| --- | --- |
| `dist\controlify-2.0.3+1.20.1-forge.jar` | 见 `dist\SHA256SUMS.txt` |

该 JAR 对应源码当前状态；重新构建后哈希会变化，属正常现象。

---

## 7. 不要提交 / 不要外发的内容

- `.gradle\`、`build\`、`versions\*\build\`（构建缓存，体积大且与本机绑定）
- `run\`、`runserver\`（开发运行时产生的存档与日志）

---

## 8. 上游信息

- 上游项目：Controlify（isXander），LGPL-3.0-or-later
- 本包基于 2.0.3 版本源码 + 针对 FST（RiaFst，MC 1.20.1 / Forge 47.4.0）的适配改动
- 本分支仅为 FST 提供支持，不保证其它整合包/服务端可用；通用需求请转上游
