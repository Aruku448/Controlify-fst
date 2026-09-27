# Controlify — FST 特供支持版本

> ## ⚠️ 本仓库仅为 FST 提供支持
>
> 这是一个 **FST 专用**的 Controlify 分支，**不是 Controlify 的通用发行版**。
>
> - 只针对 **FST 整合包 / RiaFst 客户端实例**（Minecraft **1.20.1**，Forge **47.4.0**）适配与测试
> - **不保证**与其它整合包、服务器、光影或模组组合兼容
> - **不接受**通用功能请求、上游同步请求或兼容性 issue
> - 想要通用的手柄支持，请前往上游 **[Controlify](https://github.com/isXander/Controlify)** 下载官方版本
>
> 面向 FST 开发者的环境搭建与构建说明见 **[README-DEV.md](README-DEV.md)**（Windows / Linux / macOS）。

---

## 基础

本分支基于 Controlify **2.0.3** 源码，在其之上加入 FST 所需的平台适配与针对性功能增强，
用于让 FST 客户端在 1.20.1 Forge 环境下获得完整的手柄操作体验。

| 项目 | 值 |
| --- | --- |
| 基础版本 | Controlify 2.0.3 |
| Minecraft | 1.20.1 |
| 加载器 | Forge 47.4.0 |
| 目标实例 | RiaFst |

### 维护 / 构建范围

上游持续更新，本分支不再追随。`versions/` 已精简为**两个构建目标**：

| 目标 | Minecraft | 平台 | 说明 |
| --- | --- | --- | --- |
| `1.20.1-forge` | 1.20.1 | Forge | FST 现行目标，`versions/current` 默认指向此项目 |
| `1.21-neoforge` | 1.21.1 | NeoForge | 维护范围上限 |

其余平台（Fabric、以及 1.20.4 / 1.20.6 / 1.21.3 / 1.21.4 / 1.21.5）已从 `versions/` 与
`versions/builds.json` 中移除，不再构建。

---

## 相对上游的改动

### 1. Forge 1.20.1 平台支持

- 平台适配层：`src/main/java/dev/isxander/controlify/platform/{forge,client/forge,main/forge,network/forge}/`
- 入口与 Mixin 清单：`ControlifyBootstrap.java`、`controlify-platform.forge.mixins.json`
- Forge 资源监听器在 mod 构造阶段注册，避免错过首次资源重载
- Mixin 通过 JAR 清单注册并提供 refmap；Forge 回调使用 SRG 方法目标
- Controlify 是客户端 mod，服务器无需安装

### 2. Create: Pneumatic Tacticals（CPT）兼容

代码位于 `src/main/java/dev/isxander/controlify/compatibility/cpt/`

| 功能 | 说明 |
| --- | --- |
| 反射访问 | `CptReflection` 用反射调用 CPT 公开 API，避免把 CPT 普通类当作 Mixin 接口目标而崩溃 |
| 持枪检测 | 仅在主手持有 CPT `GeoGunItem` 时启用枪械逻辑 |
| 右摇杆选弹 | 兼容 CPT 原生按键处理，只扩展手柄输入 |
| 震动 | 按弹药伤害缩放的马达震动（含左右扳机马达）；配置 `config/controlify/cpt-rumble.json`，调参工具 `tools/cpt-rumble-tuner.html` |
| 按键优先级 | 持枪时 **CPT 按键优先于冲突的 Controlify 默认绑定**（不论该 CPT 键是内置预设还是玩家手动改绑）；未持枪时保留原版按键行为（`bindings/InputBindingImpl.java` + `compatibility/cpt/CptKeyBindings.java`） |

### 3. CPT 默认手柄键位（已内置）

`src/main/resources/assets/controlify/controllers/default_bind/default.json`

| CPT 功能 | 默认按键 |
| --- | --- |
| 换弹 `reload` | `button/west`（X / □） |
| 射击模式 `fire_mode` | `dpad_right` |
| 切换弹药 `cycle_ammo` | `dpad_up` |

安装后即获得相同默认键位；玩家已有的手动改绑不会被覆盖。

这三组预设会与 Controlify 自己的默认键位撞车：`reload`↔`swap_hands`、
`cycle_ammo`↔`open_chat`、`fire_mode`↔`radial_menu`。持枪时 CPT 按键优先，冲突的默认绑定会被压制；
不持枪时一切照旧。改动这段优先级逻辑后，可用 `tools/cpt-binding-conflicts.py` 对一份实例配置做检查。

### 4. 辅助瞄准（Aim Assist）

实现：`src/main/java/dev/isxander/controlify/ingame/AimAssist.java`
（通过 `ControlifyEvents.LOOK_INPUT_MODIFIER` 修改视角输入）

- **触发**：按住瞄准输入时生效——弓 / 弩 / 三叉戟 / 望远镜，或 CPT Aim Stance / LT；右摇杆居中也持续吸附
- **默认上限**：强度 60%、视野角度 30°、距离 128 格
- **目标筛选**：视野内、无遮挡、`isPickable` 的存活生物
- 开关与参数位于「手柄设置 → 灵敏度」分组

### 5. 其它修复

- 修复输入字体映射未加载导致的绑定界面 NPE
- 修复 YACL 绑定控件的 `ArithmeticException: / by zero`
- 修复打开物品栏时 Mixin 无效导致的崩溃（屏幕 mixin 改为接口方式）
- 修复虚拟键盘键帽贴图（Forge 1.20.1 改用 `GuiGraphics.blit` 九宫格绘制）
- 补齐简体中文绑定说明与设置页绑定名称渲染

---

## 构建

```bash
# Linux / macOS
./gradlew chiseledBuildAndCollect

# Windows
gradlew.bat chiseledBuildAndCollect
```

产物：`build/finalJars/controlify-2.0.3+1.20.1-forge.jar`
（另有 `-offline.jar`，已内置前置依赖，可直接丢进 `mods` 测试）

需要 **JDK 21**；首次构建需联网。Windows 用户可直接双击 `build-forge-1.20.1.bat`。

---

## 分发与许可

- 上游 Controlify 采用 **LGPL-3.0-or-later**，详见 [LICENSE](LICENSE)；本分支的修改遵循同一许可
- 请勿将本分支产物作为官方 Controlify 发布或再分发到公开平台
