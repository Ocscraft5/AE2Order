# AE2 Order Helper / AE2 下单助手

> 在 JEI 书签或物品列表上按鼠标中键，直接为该网络下单（支持物品与流体）。
> Middle-click a JEI bookmark or item-list entry to order it from your ME network — items and fluids alike.

- **mod id / namespace**：`ae2order`
- **MC / Forge**：1.19.2 / 43.5.1
- **许可证 License**：LGPL-3.0-or-later（见 [`LICENSE`](LICENSE)、[`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)）

---

## 中文

### 它做什么

1. 打开任意 AE2 终端（有线 ME 终端、无线终端、合成终端、样板终端……），指向 JEI 书签栏或右侧物品列表里的某个东西，**按鼠标中键** → 直接弹出 AE2 的「合成数量」界面。
2. **支持流体**：书签里的水、岩浆等流体同样可以下单。
3. **只要身上带着终端就能下单**：不用打开终端界面。背包/快捷栏/副手/Curios 饰品槽里有已绑定的无线终端时，在任何能看见 JEI 书签的界面里中键点击都能下单，订单会走**该终端绑定的那个网络**。
   - 优先级：若当前正开着 AE2 终端界面，用**当前终端**的网络；否则用身上第一个已绑定的无线终端。
   - 多网络玩家请把要用的终端拿在主手/快捷栏第一位（查找顺序：主手 → 副手 → 快捷栏 → 背包 → 护甲 → Curios）。

### 安装

1. 安装 Forge 1.19.2-43.5.1。
2. 把 `mods/` 里放上：**AE2**、**AE2WTLib**（及其前置 Cloth Config / Architectury / Curios）、以及可选的 **JEI**。
3. 把 `ae2order-0.1.0+mc1.19.2-forge.jar` 丢进 `mods/`。

### 自检命令

`/ae2order selftest`（需要 OP，权限等级 2）：用你当前的携带状态，分别对「主手物品」和「水（流体）」各调一次下单流程，结果直接发到聊天栏，便于判断是终端没绑定、终端不在范围、还是没权限。

### 已知限制

- 数量固定为 1（弹出的是 AE2 原生的合成数量界面，可再手动改）。
- 只识别物品与流体；其它模组的自定义 JEI 类型会被忽略。
- AE2 的安全权限（CRAFT）与无线终端的电量/距离检查仍然生效。

---

## English

### What it does

1. Open any AE2 terminal (wired ME terminal, wireless terminal, crafting/pattern terminal…), point at an entry in the JEI bookmark panel or the item list, and **middle-click** → AE2's craft-amount screen opens for it.
2. **Fluids supported**: water, lava, … bookmarked fluids can be ordered the same way.
3. **Order just by carrying a terminal**: no terminal GUI needed. With a linked wireless terminal in your inventory, hotbar, offhand or a Curios slot, middle-clicking a JEI entry in any screen opens the order screen for **the network that terminal is linked to**.
   - If an AE2 terminal screen is open, that terminal's network is used; otherwise the first linked wireless terminal found is used (search order: main hand → offhand → hotbar → inventory → armor → Curios).

### Install

Forge 1.19.2-43.5.1 + AE2 + AE2WTLib (with Cloth Config / Architectury / Curios) + optional JEI, then drop the jar into `mods/`.

### Self-test

`/ae2order selftest` (OP level 2) runs the order flow for the item in your hand and for water, and reports the result in chat.

### Known limits

- Amount is fixed at 1 (AE2's own craft-amount screen still lets you change it).
- Items and fluids only; custom JEI ingredient types are ignored.
- AE2's CRAFT permission and the wireless terminal's power/range checks still apply.

---

## 构建 Build

```bash
# JDK 17
./gradlew clean dist        # -> dist/ae2order-0.1.0+mc1.19.2-forge.jar (+ -sources.jar)
```

开发运行：`./gradlew copyDeps runClient` / `./gradlew copyDeps runServer`。

## 仓库结构 Repository layout

```
.
├── build.gradle / settings.gradle / gradle.properties   # ForgeGradle 6 + Forge 1.19.2
├── gradle/wrapper/                                      # Gradle 8.4 wrapper（gradlew 可直接用）
├── src/main/java/com/xiadan/ae2order/                   # 模组源码（11 个类）
├── src/main/resources/                                  # mods.toml / pack.mcmeta / 中英语言文件
├── docs/DEPENDENCIES.md                                 # 依赖清单（含版本区间）
├── docs/VERIFY.md                                       # 验证记录（实测日志与结论）
├── LICENSE / GPL-3.0.txt                                # LGPL-3.0-or-later + 其要求的 GPL-3.0 全文
├── THIRD-PARTY-NOTICES.md                               # 第三方项目、许可证与商标声明
└── .gitignore                                           # 忽略 build/、run/、dist/、hc/ 等
```

## 许可 License

本模组自身代码：**LGPL-3.0-or-later**。它只调用 AE2 / JEI / AE2WTLib / Curios 的公开 API，
不打包它们的任何代码与资源；第三方项目与商标声明见 [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)。
