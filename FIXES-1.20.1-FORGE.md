# 1.20.1 Forge 修复记录

本文件记录本分支（FST 特供 1.20.1 Forge 支持版）相对于上游的**本地修复**，面向本分支的开发者。
每条修复包含：现象、根因、改动、验证方式。

> 最初的两个问题由 Windows 侧开发者排查并记录；本分支核对后确认**同样存在**，按同一根因修复，
> 并补做了可复现的验证（见各条「验证」小节）。修复提交：`c85828c`。
>
> 本分支其它改动（CPT 兼容、辅助瞄准、默认键位等）见 [README.md](README.md) 与 `git log`。

---

## 1. 「手柄设置…」按钮点不开，且首次启动不弹 SDL3 询问界面

**状态：已修复，且已完成运行时实测（见 1.5 / 1.6）；用户可见症状均已消失。**
**涉及文件：** `src/main/java/dev/isxander/controlify/mixins/core/MinecraftMixin.java`、
`src/main/java/dev/isxander/controlify/Controlify.java`

### 1.1 现象

- **选项… → 控制… → 「手柄设置…」点击后没有任何反应**（按钮会持续保持选中白框）。
- 首次启动（或 `quietMode = false` 时）**完全不会弹出 SDL3 下载询问界面**，游戏中手柄也不会被识别。
- 不崩溃、控制台没有任何异常 —— 因为异常被 `CompletableFuture.whenComplete` 静默吞掉了。

### 1.2 诊断方式

在 `Controlify.finishControlifyInit()` / `askNatives()`、`MinecraftMixin.controlify$registerInitialScreen`、
`Minecraft.setScreen` 上临时加日志（修复完成后已全部移除）。修复前的关键日志：

```
[Render thread/INFO] [Controlify/]: setScreen -> dev.isxander.controlify.gui.screen.SDLOnboardingScreen
[Render thread/INFO] [Controlify/]: setScreen -> net.minecraft.client.gui.screens.TitleScreen      ← 同一秒被覆盖
...（此后再无 "Finishing Controlify init..."）
```

点击按钮后的堆栈（被 `whenComplete` 吞掉，正常日志里看不到）：

```
java.util.NoSuchElementException: No value present
        at java.util.Optional.orElseThrow(Optional.java:377)
        at dev.isxander.controlify.gui.screen.ControllerCarouselScreen.<init>(ControllerCarouselScreen.java:83)   // getControllerManager().orElseThrow()
        at dev.isxander.controlify.gui.screen.ControllerCarouselScreen.lambda$openConfigScreen$1(ControllerCarouselScreen.java:95)
        at java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
        at dev.isxander.controlify.gui.screen.ControllerCarouselScreen.openConfigScreen(ControllerCarouselScreen.java:92)
        at net.minecraft.client.gui.screens.controls.ControlsScreen.openControllerSettings(ControlsScreen.java:575)
```

### 1.3 根因（两个缺陷叠加）

#### A. ≤1.20.1 没有 `addInitialScreens`，启动阶段的 `setScreen` 会被 MC 自己覆盖

- 1.20.1 的 `Minecraft` **没有** `addInitialScreens`；初始界面（TitleScreen / quick-play）是在
  `Minecraft.setInitialScreen(...)` 内、**`onGameLoadFinished()` 之后**才设置的。
  （已用 `javap` 核对 1.20.1 字节码：`setInitialScreen` 内部才 `setScreen(new TitleScreen(true))`。）
- Controlify 的初始化入口是 `MinecraftMixin.initControlifyNow`（注入 `onGameLoadFinished` 的 RETURN）
  → `Controlify.initializeControlify()`（`quietMode=false` 时）→ `finishControlifyInit()`
  → `askNatives()` → `InitialScreenRegistryDuck.registerInitialScreen(...)` 注册 SDL 询问界面。
- 旧实现在 ≤1.20.1 上把 `doNow` 强制成 `true`，于是**立刻** `setScreen(SDLOnboardingScreen)`；
  紧接着 MC 执行 `setInitialScreen` 设置 TitleScreen，把询问界面直接顶掉。
- 结果：询问界面从未真正展示 → `nativeOnboardingFuture` 永远不会完成 → `finishControlifyInit()` 的回调从不执行
  → `Controlify.controllerManager` 始终为 `null`（`getControllerManager()` 返回 empty），同时也意味着
  **整局游戏都没有手柄支持**（只有 quiet 模式 + 热插拔路径不受影响）。

#### B. `finishControlifyInit()` 会返回一个“假的已完成” future

- 旧实现：`if (finishedInit) return CompletableFuture.completedFuture(null);`
  但 `finishedInit` 在 `askNatives()` 完成**之前**就被置为 `true`。
- 于是设置按钮调用 `finishControlifyInit().whenComplete(...)` 时回调立刻执行（其实初始化还没做完），
  进入 `new ControllerCarouselScreen(parent)` → `controlify.getControllerManager().orElseThrow()` 抛异常。
- 异常发生在 `whenComplete` 的回调内，只会进入该 `whenComplete` 返回的 future；而调用方没有观察它
  （`openConfigScreen` 的 `th` 被忽略）→ **完全静默**，表现为“按钮点了没反应”。

### 1.4 修复内容

**`mixins/core/MinecraftMixin.java`**

1. 删除 ≤1.20.1 的 `doNow = true` 特例，和其他版本一样使用 `doNow = initialScreensHappened`。
2. 新增 ≤1.20.1 专用的 `@Inject(method = "setInitialScreen", at = @At("RETURN"))`
   → `controlify$showInitialScreens`：此时 MC 的初始界面已经就位，把 `initialScreenCallbacks`
   里排队的界面依次显示（支持多个界面链式显示，全部结束后回到原界面）。
3. 该注入点之后才注册的界面（例如用户点「手柄设置…」时触发的 SDL 询问）依旧是立即显示，
   与 >1.20.1 的行为一致。

**`Controlify.java`**

- `private boolean finishedInit` → `private CompletableFuture<Void> finishInitFuture`。
  重复调用 `finishControlifyInit()` 返回**同一个进行中的 future**，从而保证不变量：
  **future 完成 ⇒ `controllerManager` 已就绪**。

### 1.5 验证

| 场景 | 结果 |
| --- | --- |
| 修复前（全新配置） | `setScreen -> SDLOnboardingScreen` 之后同一秒 `setScreen -> TitleScreen`；无 `Finishing Controlify init...`；点按钮抛 `NoSuchElementException`（静默） |
| 修复后（全新配置，始终有效） | `setScreen -> TitleScreen` → `setScreen -> SDLOnboardingScreen` → 选择“是” → `DownloadingSDLScreen` → `Finishing Controlify init...` → `Discovering and initializing controllers...`；随后 选项→控制→「手柄设置…」成功打开 `ControllerCarouselScreen`（可继续进入 `ControllerCalibrationScreen`），全程 0 异常 |
| 修复后（SDL 已下载的情形） | 启动即 `Finishing Controlify init...` + `Discovering and initializing controllers...`，无询问界面 |

本分支另做的**静态**验证（可复现，见第 3 节）：

| 检查 | 结果 |
| --- | --- |
| 1.20.1 生成源码 | `>1.20.1` 分支被注释、`setInitialScreen` 注入为活代码 |
| 1.20.1 refmap | `MinecraftMixin :: setInitialScreen -> Lnet/minecraft/client/Minecraft;m_278684_(Lcom/mojang/realmsclient/client/RealmsClient;Lnet/minecraft/server/packs/resources/ReloadInstance;Lnet/minecraft/client/main/GameConfig$QuickPlayData;)V` —— 描述符完整，说明 Mixin 确实解析到了真方法 |
| 1.20.1 编译产物 | 只有 `controlify$showInitialScreens` / `controlify$showNextInitialScreen`，无 `injectCustomInitialScreens` |
| 1.21-neoforge 编译产物 | 反之：只有 `injectCustomInitialScreens` —— 未破坏另一目标 |
| `Controlify.class` | `finishedInit` 字段消失，改为 `finishInitFuture` |

### 1.6 运行时实测（本分支已完成）

本机（Linux）发行版自带 SDL3（`/usr/lib/libSDL3.so.0.4.16`），`tryOfflineLoadAndStart()` 总是成功，
`askNatives()` 在第一道 `if` 就返回，**询问界面分支根本不可达** —— 所以只改 `vibration_onboarded` 没用
（两种取值都是 `true && true`）。实测时临时把该 `if` 短路掉（`false && ...`，带 `[TEST-ONLY-TEMP]` 标记），
测试完已还原并重建正式产物。

**实测结果：通过。** 关键判据是配置写回：

```
测试前  global.vibration_onboarded = false   ← 手工置入，两种写入路径中只有这一条可能被走到
[22:50:19] Initializing Controlify...
[22:50:19] Loading Controlify config...
[22:50:29] Saving Controlify config...                       ← 询问界面回答处理里回写
[22:50:29] [SDL3NativesManager] Loading SDL3 version: 3.4.16
[22:50:29] Finishing Controlify init...                     ← future 真的完成了
[22:50:29] No controllers found.
[22:50:40] [ControllerManager] Controller connected: '8BitDo Ultimate 3-mode Controller for Xbox'#SDL-1-HID[...]
测试后  global.vibration_onboarded = true
```

为什么这能证明修复生效：`vibrationOnboarded = true` 全仓库只有两个写入点 ——
`Controlify.askNatives()` 的离线短路分支（测试时已被短路掉）与
`SDLOnboardingScreen` 的回答处理（`SDLOnboardingScreen.java:15`）。
既然短路分支不可达而配置仍被从 `false` 改成 `true`，只能是**询问界面确实显示并被回答了**。
修复前该界面会被 `setInitialScreen` 的 TitleScreen 顶掉，future 永挂，
`Finishing Controlify init...` 不会出现，配置也不会被回写。

同一会话中手柄随后被识别（`8BitDo Ultimate 3-mode Controller for Xbox`），
说明初始化链路完整跑通；日志无任何 `Controlify` 相关异常。

用户确认「选项 → 控制… → 手柄设置…」也能正常打开，即**用户可见症状已全部消失**。

> 但要说清楚：**这一步并不能单独证明缺陷 B 已修**。点按钮时初始化早已完成，
> 此时 `controllerManager` 已就绪，旧代码（`finishedInit` + `completedFuture(null）`）
> 在同一个场景下也会正常工作。
> 缺陷 B 只在乎「初始化未完成时打按钮」那个窗口，而它被模态的询问界面挡住了，
> 本机无法构造。该条仍仅依靠不变量保证：**重复调用 `finishControlifyInit()` 返回
> 同一个进行中的 future ⇒ future 完成 ⇔ `controllerManager` 已就绪**。

### 1.7 影响面与注意事项

- 改动全部包在 stonecutter 的 `/*? if <=1.20.1 {*/ … /*?}*/` 中（`doNow` 特例是删除），
  **>1.20.1 的渲染继续走 `addInitialScreens`，行为不变**。
- 交互顺序变化：1.20.1 上 SDL3 询问界面现在出现在标题界面**之后**（与 1.20.2+ 一致）。
- 教训：`whenComplete` 回调内抛出的异常会进入其返回的 future，若无人观察就完全静默。
  凡是“等初始化完成再继续”的调用方（如 `ControllerCarouselScreen.openConfigScreen`），
  必须保证 future 完成时依赖已就绪 —— 这也是缺陷 B 修成“memoize 真实 future”的原因。
- 未改动：`GlobalSettings.quietMode`（默认 `false`）与 `probeMode` / 热插拔探测逻辑；
  没有检测到手柄时轮播界面仍按原设计进入空状态（`controllerNotDetectedButton`）。

---

## 2. dev client 启动即崩溃（Legacy Forge + YACL 重混淆）

**状态：已修复。**
**涉及文件：** `build.gradle.kts`

### 2.1 现象

1.20.1 Forge dev client 启动到资源重载阶段崩溃：`AbstractMethodError`，
YACL 把 `PreparableReloadListener#reload` 实现成了 SRG 名 `m_5540_`。

### 2.2 根因

Legacy Forge 发布的 mod jar 重混淆为 SRG **成员**名（类名保持命名），而 dev client 跑在命名
（Mojang）映射下。MDG 只提供 named → SRG（重混淆）方向，不会把 SRG 依赖反混淆。

已核对本分支确实命中该前置条件：

- Gradle 缓存里的 `yet-another-config-lib-3.6.6+1.20.1-forge.jar`，其
  `dev/isxander/yacl3/gui/image/YACLImageReloadListener` 实现的是 `m_5540_(...)`，不是 `reload(...)`
- 修复前 `build.gradle.kts` 用 `modstitchModApi` 引入 YACL，全仓库没有任何 SRG → named 重映射
- Gradle transforms 缓存中查不到 YACL，说明 MDG 并未替我们重映射它

### 2.3 修复内容

forge 平台上：

1. YACL 改为 `modstitchModCompileOnly`（编译期可见、不进 dev 运行时）；
2. 新增任务 `remapYaclToNamed`：MDG `RemapJar` + `ObfuscationExtension.configureSrgToNamedOperation`
   （即 installertools + `intermediateToNamed.srg`）把 SRG jar 重映射为命名 jar；
3. 产物以 `runtimeOnly` 提供给 dev 运行时。

发布产物不受影响：YACL 不打包，`mods.toml` 的依赖声明来自模板
（`src/main/templates/META-INF/mods.toml`），与依赖配置方式无关。

**API 位置备忘**（本仓库实际使用的是 Modstitch 0.5.12 → MDG **`legacyforge` 2.0.74**）：

```
net.neoforged.moddevgradle.legacyforge.tasks.RemapJar            // getInput / getLibraries / getRemapOperation
net.neoforged.moddevgradle.legacyforge.tasks.RemapOperation      // getToolType / getMappings / getToolClasspath
net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension  // configureSrgToNamedOperation / getSrgToNamedMappings
```

> 注意：旧版 `net.neoforged.moddevgradle.legacy`（MDG 2.0.6x，包名不带 `forge`）**没有**
> `configureSrgToNamedOperation`。别照着旧包名找这个 API。
>
> 在 Kotlin DSL 的任务配置块里 `property("deps.yacl")` 会解析到 `Task.property(...)`，
> 必须写 `project.property(...)`（或提前取到局部变量）。

### 2.4 验证

| 检查 | 结果 |
| --- | --- |
| `remapYaclToNamed` 产物 | `build/devRemappedMods/yet-another-config-lib-3.6.6+1.20.1-forge-named.jar` |
| 产物内方法名 | `m_5540_` → **`reload(`** |
| compileClasspath | 原始 SRG 版 YACL（`~/.gradle/caches/.../yet-another-config-lib-3.6.6+1.20.1-forge.jar`） |
| runtimeClasspath | 重映射后的命名版；**无未重映射的 YACL 残留** |
| 发布 jar | `mods.toml` 仍声明 `yet_another_config_lib_v3`；jar 内不含 `dev/isxander/yacl3`（未误打包） |
| 构建 | 1.20.1-forge 与 1.21-neoforge 均 BUILD SUCCESSFUL |

**未做**：实际启动 dev client 跑一遍（静态链路已完整，但没跑过 `runClientActive`）。

---

## 3. 缺前置（YACL）时不弹缺前置界面、直接崩溃

**状态：已修复。**
**涉及文件：** `src/main/templates/META-INF/mods.toml`、`src/main/templates/META-INF/neoforge.mods.toml`
**影响目标：** 1.20.1-forge（主目标）；1.21-neoforge 同一模板写法，同样受影响

### 3.1 现象

只把 `controlify-2.0.3+1.20.1-forge.jar` 放进 `mods/`（**未装** YACL）时：

- **不会**出现 Forge 的 `Missing or unsupported mandatory dependencies` 界面；
- 游戏直接崩溃，crash-report：

```
java.lang.NoClassDefFoundError: Could not initialize class dev.isxander.controlify.config.GlobalSettings
        at dev.isxander.controlify.config.ControlifyConfig.<init>(ControlifyConfig.java:45)
        at dev.isxander.controlify.Controlify.<init>(Controlify.java:90)
        at net.minecraftforge.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:77)

Caused by: java.lang.ExceptionInInitializerError:
   Exception java.lang.NoClassDefFoundError: dev/isxander/yacl3/api/NameableEnum
```

即本应由 Forge 在 mod 加载前拦下的“缺前置”，变成了加载期的 `NoClassDefFoundError`。

### 3.2 根因

**依赖表的表头带了引号**，导致两个依赖声明在 TOML 层面根本没构成依赖表：

```toml
[["dependencies.${mod_id}"]]      # ← 问题所在
```

按 TOML 规范，带引号的 `"dependencies.controlify"` 是**键名里含点号的单个键**，并不等于
`dependencies` → `controlify` 的嵌套表。本分支用 `tomllib` 解析构建产物直接看到：

```
top-level keys          : ['dependencies.controlify', 'license', 'loaderVersion', 'mixins', 'modLoader', 'mods']
有嵌套 dependencies 表 : False      ← Forge 就是查这个表
有扁平的点号键         : True
```

Forge 读依赖的方式是查 `dependencies` 表 → 查不到 → **`minecraft` 与 `yet_another_config_lib_v3`
两个依赖全部失效**。决定性日志（修复前）：

```
[ModSorter/LOADING]: Found 0 mod requirements missing (0 mandatory, 0 optional)
...
Mod List:
    main | Controlify | controlify | 2.0.3+1.20.1-forge | ERROR | Manifest: NOSIGNATURE
```

依赖不生效 ⇒ 缺 YACL 时 Forge 认为“没有依赖缺失” ⇒ 照常构造 mod ⇒
构造期初始化 `GlobalSettings`（字段 `ReachAroundMode` 引用了 `dev.isxander.yacl3.api.NameableEnum`）
⇒ `NoClassDefFoundError` 崩溃。

> **来源**：该写法继承自上游 *isXander/Controlify 2.0.3* 的模板。上游主要面向 Fabric
> （依赖写进 `fabric.mod.json`，由 Fabric Loader 强制），这份 Forge 模板显然没有被真正验证过；
> 本分支是 Forge 1.20.1 专用，于是暴露出来。同类问题也适用于任何直接复用该模板的 Forge/NeoForge 移植。

### 3.3 修复

模板本身**不参与 TOML 解析**（`${mod_id}` 由构建期字符串替换），因此去掉引号、写成裸键即可：

```diff
- [["dependencies.${mod_id}"]]
+ [[dependencies.${mod_id}]]
```

同时给 YACL 依赖补上显式版本范围（原先缺 `versionRange`，Forge 报错里的 *Expected range* 是空串）：

```diff
  [[dependencies.${mod_id}]]
  modId = "yet_another_config_lib_v3"
  mandatory = true
+ versionRange = "[0,)"
  ordering = "NONE"
  side = "BOTH"
```

`neoforge.mods.toml` 做同样处理，并按 NeoForge 规范把 `mandatory = true` 换成 `type = "required"`
（NeoForge 用 `type`，取值 `required` / `optional` / `incompatible` / `discouraged`；
`type` 缺省值本来就是 `required`，所以即使解析器忽略它也不会变错）。
两份模板里都加了注释说明为什么表头不能带引号。

### 3.4 验证

本分支复检四个构建产物（`tomllib` 解析 `build/finalJars/*.jar`）：

```
controlify-2.0.3+1.20.1-forge.jar          嵌套 dependencies=有  扁平点号键=无
   ('minecraft', '[1.20.1,1.20.2)', True)
   ('yet_another_config_lib_v3', '[0,)', True)
controlify-2.0.3+1.20.1-forge-offline.jar  同上
controlify-2.0.3+1.21-neoforge.jar         嵌套 dependencies=有  扁平点号键=无
   ('minecraft', '[1.21,1.21.1]', 'required')
   ('yet_another_config_lib_v3', '[0,)', 'required')
controlify-2.0.3+1.21-neoforge-offline.jar 同上
```

原作者在 dev 运行时以「缺 YACL」「有 YACL」两种场景实测：

| 场景 | 关键日志 | 结果 |
| --- | --- | --- |
| 修复前 + 缺 YACL | `Found 0 mod requirements missing (0 mandatory, 0 optional)` | 继续构造 mod → `NoClassDefFoundError`，产生 crash-report ❌ |
| 修复后 + 缺 YACL | `Found 1 mod requirements missing (1 mandatory, 0 optional)`<br>`Mod ID: 'yet_another_config_lib_v3', Expected range: '[0,)'` | 正常进入 Forge 缺前置报错流程，**不再崩溃** ✅ |
| 修复后 + 有 YACL | `Found 0 mod requirements missing` → `Finishing Controlify init...` | 正常启动，`[0,)` 不会误判已安装的前置 ✅ |

### 3.5 影响面

- 只改 mod 元数据，**不改任何代码路径**，对已装齐前置的玩家无行为变化。
- 之前“依赖声明失效”是**静默**的：`minecraft` 的版本范围校验同样没生效。修复后也会真正生效
  （range 由模板的 `${mc}` 提供，本分支 Forge 为 `[1.20.1,1.20.2)`）。
- 版本范围取 `[0,)`：只强制“存在”，不做 YACL 版本比对。若要卡最低版本，
  把它换成例如 `[3.6.6,)` 即可（注意 Maven 版本比较对 `3.6.6+1.20.1-forge`
  这类带构建元数据的版本串的处理）。

---

## 4. 复现 / 验证命令速查

```bash
# 只构建 forge 目标（CI_SINGLE_BUILD 让 Stonecutter 只注册该版本）
CI_SINGLE_BUILD=1.20.1-forge ./gradlew chiseledBuildAndCollect

# 看 1.20.1 实际编译的源码（Stonecutter 把不生效的分支注释掉）
grep -n -A20 'setInitialScreen' \
  versions/1.20.1-forge/build/chiseledSrc/main/java/dev/isxander/controlify/mixins/core/MinecraftMixin.java

# 看 refmap 是否解析到真方法
python3 -c "import json;d=json.load(open('versions/1.20.1-forge/build/mixin/controlify.refmap.json'));\
print([ (k,v) for k,v in d['mappings'].items() if 'MinecraftMixin' in k ])"

# 单独跑 YACL 重映射并检查方法名
./gradlew :1.20.1-forge:remapYaclToNamed
unzip -p versions/1.20.1-forge/build/devRemappedMods/*.jar \
  dev/isxander/yacl3/gui/image/YACLImageReloadListener.class > /tmp/y.class
javap -p /tmp/y.class | grep -E 'm_5540_|reload\('     # 应为 reload(
```

---

## 5. 上游信息

### 5.1 给上游的说明（English）

> **Forge/NeoForge metadata bug: dependency table header is quoted**
>
> `src/main/templates/META-INF/mods.toml` declares `[["dependencies.${mod_id}"]]`. In TOML a quoted key
> is a *single key containing dots*, so the file ends up with a flat top-level key
> `dependencies.controlify` instead of a nested `dependencies` → `controlify` table. Forge reads
> dependencies from the `dependencies` table, so **no dependency is registered at all**
> (`Found 0 mod requirements missing` in the log). Consequence: with `yet_another_config_lib_v3`
> absent the game skips the "Missing or unsupported mandatory dependencies" screen, constructs the mod,
> and dies with `NoClassDefFoundError: dev/isxander/yacl3/api/NameableEnum`. The same applies to
> `neoforge.mods.toml` (where `mandatory` should also be `type = "required"`).
>
> Fix: use an unquoted table header — `[[dependencies.${mod_id}]]` — and give the dependency an explicit
> `versionRange` (e.g. `"[0,)"`); otherwise Forge reports an empty *Expected range*.
> Verified on Forge 1.20.1 (47.4.0): before the fix the log says
> `Found 0 mod requirements missing` and the game crashes; after the fix it says
> `Found 1 mod requirements missing (1 mandatory, 0 optional)` and Forge shows its missing-dependency
> error screen instead.

### 5.2 本分支

- 上游项目：Controlify（isXander），LGPL-3.0-or-later
- 本文件的修复均为本分支为 1.20.1 Forge 支持的本地改动，未回馈上游
- 本分支仅为 FST 提供支持，不保证其它整合包/服务端可用；通用需求请转上游
