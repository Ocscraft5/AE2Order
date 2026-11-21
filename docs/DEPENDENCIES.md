# 依赖清单 / Dependencies

**AE2 Order Helper (`ae2order`) 0.1.0 — Minecraft 1.19.2 / Forge 43.5.1**

把 jar 放进 `mods/` 时，下面的“必需”一个都不能少；“可选”不装也能启动（对应功能会自动关闭）。

## 必需 / Required

| 模组 | mod id | 版本 | 下载位置 | 说明 |
|---|---|---|---|---|
| Minecraft Forge | `forge` | **1.19.2-43.5.1** | https://files.minecraftforge.net/ | 加载器 |
| Applied Energistics 2 | `ae2` | **12.8.4**（可用区间 `[12.8.4,13.0.0)`） | https://modrinth.com/mod/ae2 / CurseForge | 终端、下单界面、网络 |
| AE2WTLib | `ae2wtlib` | **12.9.7**（可用区间 `[12.9.0,13.0.0)`） | https://modrinth.com/mod/ae2wtlib | 无线终端（本模组用它的 CurioLocator） |
| Cloth Config API | `cloth_config` | **8.3.134** | https://modrinth.com/mod/cloth-config | AE2WTLib 的强制前置 |
| Architectury API | `architectury` | **6.5.90** | https://modrinth.com/mod/architectury-api | AE2WTLib 的强制前置 |
| Curios API | `curios` | **1.19.2-5.1.6.4** | https://modrinth.com/mod/curios | AE2WTLib 的强制前置；也是本模组“饰品槽里的终端”判定的 API |

> AE2WTLib 的 `META-INF/mods.toml` 里 cloth-config / architectury / curios 三者 `mandatory=true`，
> 缺任意一个，游戏会直接报 "Missing or unsupported mandatory dependencies"。

## 可选 / Optional

| 模组 | mod id | 版本 | 说明 |
|---|---|---|---|
| Just Enough Items | `jei` | **11.2.0 – 11.x**（`[11.2.0,12)`） | 没有 JEI 时本模组照常启动，只是“中键下单”没有可点的东西 |

jei 依赖声明为 `side="CLIENT"`：**专用服务器上不需要装 JEI**，本模组在服务端不会触碰 JEI 的类
（实测：无 JEI 的专用服务器启动日志出现 `JEI not loaded, order-from-JEI disabled`，无 NoClassDefFoundError）。

## 版本对照（本机实测）

| 项目 | 值 |
|---|---|
| JDK | 17（`F:\alljava\17`） |
| Gradle | 8.4 |
| ForgeGradle | `[6.0,6.2)` |
| Mappings | `official` / `1.19.2` |
| 发布 jar | `ae2order-0.1.0+mc1.19.2-forge.jar`（已 reobf，可直接进生产环境 `mods/`） |
