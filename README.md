# Controlify — FST 分支（1.20.1 Forge 适配）

这是 [Controlify](https://github.com/isXander/Controlify) **2.0.3** 的本地分支，
为 Minecraft **1.20.1 / Forge 47.4.0** 增加了平台适配，并针对
[Create: Pneumatic Tacticals](https://modrinth.com/mod/create-pneumatic-tacticals)（CPT）
与手柄辅助瞄准做了针对性增强。

> 面向 Windows 开发者的完整环境说明见 **[README-WINDOWS.md](README-WINDOWS.md)**。

---

## 这个分支做了什么

### 1. Forge 1.20.1 平台支持

- 平台适配层：`src/main/java/dev/isxander/controlify/platform/{forge,client/forge,main/forge,network/forge}/`
- 入口与 Mixin 清单：`ControlifyBootstrap.java`、`controlify-platform.forge.mixins.json`
- Forge 资源监听器在 mod 构造阶段注册，避免错过首次资源重载
- Mixin 通过 JAR 清单注册并提供 refmap；Forge 回调使用 SRG 方法目标
- Controlify 是客户端 mod，不要求服务器安装

### 2. Create: Pneumatic Tacticals 兼容

代码位于 `src/main/java/dev/isxander/controlify/compatibility/cpt/`

| 功能 | 说明 |
| --- | --- |
| 反射访问 | `CptReflection` 用反射调用 CPT 公开 API，避免把 CPT 普通类当作 Mixin 接口目标而崩溃 |
| 持枪检测 | 仅在主手持有 CPT `GeoGunItem` 时启用枪械逻辑 |
| 右摇杆选弹 | 兼容 CPT 原生按键处理，只扩展手柄输入 |
| 震动 | 按弹药伤害缩放的马达震动（含左右扳机马达），配置见 `config/controlify/cpt-rumble.json`，调参工具 `tools/cpt-rumble-tuner.html` |
| 按键优先级 | 持枪时**手动改绑的 CPT 按键优先于冲突的 Controlify 默认绑定**；未持枪时保留原版行为（`bindings/InputBindingImpl.java`） |

### 3. CPT 默认手柄键位（已内置）

`src/main/resources/assets/controlify/controllers/default_bind/default.json`

| CPT 功能 | 默认按键 |
| --- | --- |
| 换弹 `reload` | `button/west`（X / □） |
| 射击模式 `fire_mode` | `dpad_right` |
| 切换弹药 `cycle_ammo` | `dpad_up` |

其他玩家安装后即获得相同默认键位；已有手动改绑不会被覆盖。

### 4. 辅助瞄准（Aim Assist）

实现：`src/main/java/dev/isxander/controlify/ingame/AimAssist.java`
（通过 `ControlifyEvents.LOOK_INPUT_MODIFIER` 修改视角输入）

- **触发**：按住瞄准输入时生效——弓 / 弩 / 三叉戟 / 望远镜，或 CPT Aim Stance / LT；右摇杆居中也持续吸附
- **默认上限**：强度 60%、视野角度 30°、距离 128 格
- **目标筛选**：视野内、无遮挡、`isPickable` 的存活生物
- 开关与参数在「手柄设置 → 灵敏度」分组中

---

## 构建

```bash
# Linux / macOS
./gradlew chiseledBuildAndCollect

# Windows
gradlew.bat chiseledBuildAndCollect
```

产物：`build/finalJars/controlify-2.0.3+1.20.1-forge.jar`
（另有 `-offline.jar`，已内置前置依赖）

需要 **JDK 21**。首次构建需联网。

---

## 许可

上游 Controlify 采用 **LGPL-3.0-or-later**，详见 [LICENSE](LICENSE)。
本分支的修改同样遵循该许可。
