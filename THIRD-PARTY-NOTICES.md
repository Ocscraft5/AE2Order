# Third-Party Notices / 第三方声明

**AE2 Order Helper (mod id: `ae2order`)**

This mod **does not bundle, copy or redistribute** any third-party code, class files, textures,
language entries, recipes or data. Its jar contains only classes under `com.xiadan.ae2order`,
this mod's own resources (`META-INF/mods.toml`, `assets/ae2order/lang/*`), `LICENSE`,
`THIRD-PARTY-NOTICES.md` and `GPL-3.0.txt`.

It only calls **public APIs** of the projects below, which the user installs separately.

| Project | Mod id | Version used for development | License | What this mod uses |
|---|---|---|---|---|
| Minecraft Forge | `forge` | 1.19.2-43.5.1 | LGPL-2.1 | mod loader, `SimpleChannel`, screen events |
| Applied Energistics 2 (AE2) | `ae2` | 12.8.4 | LGPL-3.0-or-later (see AE2 repository `LICENSE`) | public API only: `AEKey`/`AEItemKey`/`AEFluidKey`, `MEStorageMenu`, `AEBaseMenu#getLocator`, `CraftAmountMenu#open`, `MenuLocators#forInventorySlot`, `WirelessTerminalItem`, `ITerminalHost` |
| AE2WTLib (Applied Energistics 2 Wireless Terminals) | `ae2wtlib` | 12.9.7 (`Sk6tHeLM`) | MIT | public class `de.mari_023.ae2wtlib.curio.CurioLocator` (menu locator for Curios slots) |
| Just Enough Items (JEI) | `jei` | 11.68.0.1085 (runtime: 11.2.0–11.x) | MIT | public API only: `@JeiPlugin`, `IJeiRuntime`, `IBookmarkOverlay`, `IIngredientListOverlay`, `ITypedIngredient` |
| Cloth Config API | `cloth_config` | 8.3.134 | LGPL-3.0 | not used directly; mandatory dependency of AE2WTLib |
| Architectury API | `architectury` | 6.5.90 | LGPL-3.0 | not used directly; mandatory dependency of AE2WTLib |
| Curios API | `curios` | 1.19.2-5.1.6.4 | LGPL-3.0-or-later | `CuriosApi#getCuriosHelper` to find a terminal in a curio slot |

## 中文说明

本模组**不包含**上述任何项目的代码、类文件、贴图、语言文件或数据，jar 内只有本模组自己的
`com.xiadan.ae2order` 包与自有资源。运行时只调用它们的**公开 API**，这些模组需要使用者自行安装。

- AE2 是 TeamAppliedEnergistics 的项目，本模组是**非官方附属**，与官方无隶属或背书关系。
- JEI 是 mezz 的项目；AE2WTLib 是 Mari_023 的项目；Curios 是 TheIllusiveC4 的项目。
- 本模组自身以 **LGPL-3.0-or-later** 发布（见 `LICENSE`），完整对应源码随 `-sources.jar` 提供，
  使用者可以替换/修改本模组或上述依赖中的任何一方。

## Trademarks

"Applied Energistics 2", "AE2", "Just Enough Items", "JEI", "AE2WTLib", "Curios",
"Cloth Config", "Architectury", "Minecraft", "Forge" are names/trademarks of their respective
owners. This mod is an unofficial add-on and is not affiliated with or endorsed by them.
