# 验证记录 / Verification log

**AE2 Order Helper (`ae2order`) 0.1.0 — MC 1.19.2 / Forge 43.5.1**
最后一次验证：2026-11-22 00:41–00:49（本机）

---

## 1. 构建 + reobf

| 项 | 结果 |
|---|---|
| `gradlew clean dist` | BUILD SUCCESSFUL（14 tasks） |
| 主 jar | `dist/ae2order-0.1.0+mc1.19.2-forge.jar` 41029 B |
| sources jar | 18758 B |
| reobf 是否生效 | 7 个 class 内均含 SRG 名（`m_41720_`、`f_96541_` 等）；例：`OrderClickHandler.class` 有 2 个 `m_` + 5 个 `f_` |
| jar 内容 | 只有 `com/xiadan/ae2order/*` + 自有资源 + `LICENSE`/`GPL-3.0.txt`/`THIRD-PARTY-NOTICES.md`；**无** `appeng/`、`mezz/` 的 class |
| `mods.toml` | `modId="ae2order" version="0.1.0" displayName="AE2 Order Helper / AE2 下单助手" license="LGPL-3.0-or-later"`；jei `mandatory=false side="CLIENT" versionRange="[11.2.0,12)"` |

## 2. 专用服务器（**没有 JEI**，上游 jar + 发布 jar）

`prodserver\`：Forge 1.19.2-43.5.1 官方安装器 + `--launchTarget forgeserver`，
mods = AE2 12.8.4 / AE2WTLib 12.9.7 / cloth-config 8.3.134 / architectury 6.5.90 / curios 5.1.6.4 / **ae2order 0.1.0（发布 jar）**，**无 JEI**。

实测日志（`prodserver\ae2order-prodserver.log`）：

```
[ae2order] mod constructed
[ae2order] network channel registered: ae2order:main (1 packet, protocol 2)
[ae2order] displayName=AE2 Order Helper / AE2 下单助手 version=0.1.0
[ae2order] JEI not loaded, order-from-JEI disabled
Done (2.629s)! For help, type "help"
```

- 零 `NoClassDefFoundError` / `NoSuchMethodError` / `Mixin apply failed`。
- 说明：控制台/日志文件用 GBK 写入，`下单助手` 在日志里的字节是 `CF C2 B5 A5 D6 FA CA D6`（GBK），
  按 GBK 解码即为「下单助手」——**证明 TOML 的中文转义解析正确**；开发客户端日志（UTF-8）里则直接显示中文。
- 该行同时也证明：**专用服务器不装 JEI 也能启动**（旧实现会在这里 NoClassDefFoundError，已修）。

## 3. 开发客户端 + 集成服务器（真人操作 7 次，全部成功）

客户端 `ae2order-runclient.log`，服务端（集成服务器）同一文件内的 `[Server thread]` 行：

| 时间 | 客户端请求 | 服务端结果 |
|---|---|---|
| 00:47:33 | `key=minecraft:coarse_dirt via=open-terminal containerId=1` | `opened craft amount menu: key=minecraft:coarse_dirt source=0 locator=MenuItem{slot=4}` |
| 00:48:49 | `key=minecraft:water via=open-terminal containerId=6` | `... key=minecraft:water source=0` |
| 00:48:53 | `key=minecraft:water via=open-terminal containerId=9` | `... key=minecraft:water source=0` |
| 00:48:58 | `key=minecraft:lava via=open-terminal containerId=12` | `... key=minecraft:lava source=0` |
| **00:49:00** | `key=ae2:medium_quartz_bud jei=itemlist via=carried-terminal source=1 slot=4 curio=` | `... key=ae2:medium_quartz_bud source=1 locator=MenuItem{slot=4}` |
| **00:49:01** | `key=minecraft:lava jei=itemlist via=carried-terminal source=1 slot=4` | `... key=minecraft:lava source=1` |
| 00:49:07 | `key=minecraft:oak_planks via=open-terminal containerId=20` | `... key=minecraft:oak_planks source=0` |

结论：

1. **物品 + 终端开着**（source=0）：打开合成数量界面成功。
2. **流体**（`minecraft:water`、`minecraft:lava`）：客户端识别为 `AEKey`，服务端 `CraftAmountMenu.open`
   接受并成功切到 `CraftAmountMenu`（`player.containerMenu instanceof CraftAmountMenu` 校验通过）——**流体支持实测可用**。
3. **只带终端、终端界面没开**（source=1，`slot=4` 即快捷栏第 5 格里的无线终端）：服务端用
   `MenuLocators.forInventorySlot(4)` 定位到该终端并打开了下单界面——**“只要人持有终端即可下单到对应网络”实测可用**。
4. 7 次请求 0 次 `craft request rejected`。

## 4. 事件替代 Mixin 的依据（字节码实测）

`net/minecraft/client/MouseHandler`（Forge 1.19.2-43.5.1 `injected.jar`，`javap -c`）：
先 `ForgeHooksClient.onMouseButtonPre(III)Z`，返回 true 时直接 return；否则才执行
`"mouseClicked event handler"` 的 `wrapScreenError -> screen.mouseClicked(...)`。
=> 在 `ScreenEvent.MouseButtonPressed.Pre` 里 cancel 等价于旧版 `cir.setReturnValue(true)`，
   AE2 自带的中键逻辑（`MEStorageScreen.java:453-468`）不会执行。

## 5. 未覆盖 / 已知限制

| 项 | 状态 |
|---|---|
| Curios 饰品槽里的终端（source=2） | 代码路径已实现（`CurioSupport` + `CurioLocator`），**未真人实测** |
| 发布 jar 在专用服务器上的真人点击 | 只验证了启动 + mod 加载（第 2 节）；逻辑代码与开发版完全相同，且 reobf 已核对 |
| 一次多终端（不同网络）选择 | 取“第一个找到的已绑定终端”，顺序：主手→副手→快捷栏→背包→护甲→Curios |
| 数量 | 固定 1（弹出的是 AE2 原生合成数量界面，可手动改） |
| 从旧版升级的存档 | 会提示 `Missing data pack mod:xiadan`（modid 改名所致），无实际影响 |
