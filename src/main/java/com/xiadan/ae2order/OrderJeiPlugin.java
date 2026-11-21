// 依据（JEI 插件契约，1.19.2 线全部版本一致）：
//   mezz/jei/api/JeiPlugin.java:4-7          "All IModPlugin must have this annotation and a constructor with no arguments"
//   mezz/jei/api/IModPlugin.java:34          getPluginUid()
//   mezz/jei/api/IModPlugin.java:178         default void onRuntimeAvailable(IJeiRuntime)
//   mezz/jei/api/IModPlugin.java:186         default void onRuntimeUnavailable()
// 依据（发现机制）：
//   mezz/jei/forge/startup/ForgePluginFinder.java:33,38,49-50
//     getAllScanData() 扫注解 -> 匹配 @JeiPlugin -> 反射取无参构造器
//   => 不需要主类手动注册
// 依据（取值 API）：
//   mezz/jei/api/runtime/IJeiRuntime.java:40    IIngredientListOverlay getIngredientListOverlay()
//   mezz/jei/api/runtime/IJeiRuntime.java:46    IBookmarkOverlay getBookmarkOverlay()
//   mezz/jei/api/runtime/IBookmarkOverlay.java:23   Optional<ITypedIngredient<?>> getIngredientUnderMouse()
//   mezz/jei/api/runtime/IIngredientListOverlay.java:23/30/37
//   mezz/jei/api/ingredients/ITypedIngredient.java:34  T getIngredient()
// => 同一个入口同时覆盖物品（ItemStack）与流体（FluidStack），本模组据此把二者都映射成 AEKey。
package com.xiadan.ae2order;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IIngredientListOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;

@JeiPlugin
public final class OrderJeiPlugin implements IModPlugin {

    public static final String JEI_MOD_ID = "jei";

    private static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(Ae2OrderMod.MOD_ID, "jei_order_source");

    /** 鼠标下命中的东西 + 来源（仅用于日志）。 */
    public record Hovered(AEKey key, String source) {
    }

    // volatile：onRuntimeAvailable 可能在 JEI 的工作线程回调，getter 会被渲染线程调用
    @Nullable
    private static volatile IJeiRuntime runtime;

    @Nullable
    private static volatile IBookmarkOverlay bookmarkOverlay;

    // ForgePluginFinder 走反射无参构造器，必须显式提供 public 无参构造器
    public OrderJeiPlugin() {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        bookmarkOverlay = jeiRuntime.getBookmarkOverlay();   // IJeiRuntime.java:46
        Ae2OrderMod.LOGGER.info("[{}] JEI runtime cached (bookmark overlay = {})",
                Ae2OrderMod.MOD_ID, bookmarkOverlay != null);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        bookmarkOverlay = null;
        Ae2OrderMod.LOGGER.info("[{}] JEI runtime released", Ae2OrderMod.MOD_ID);
    }

    /** 软依赖门：JEI 未加载时调用方无需判 null。 */
    public static boolean isJeiLoaded() {
        return ModList.get() != null && ModList.get().isLoaded(JEI_MOD_ID);
    }

    public static Optional<IJeiRuntime> getRuntime() {
        if (!isJeiLoaded()) {
            return Optional.empty();
        }
        return Optional.ofNullable(runtime);
    }

    public static Optional<IBookmarkOverlay> getBookmarkOverlay() {
        if (!isJeiLoaded()) {
            return Optional.empty();
        }
        return Optional.ofNullable(bookmarkOverlay);
    }

    public static Optional<IIngredientListOverlay> getIngredientListOverlay() {
        return getRuntime().map(IJeiRuntime::getIngredientListOverlay);   // IJeiRuntime.java:40
    }

    /**
     * 鼠标下（JEI 书签面板优先，其次物品/搜索列表）的 AE 键：物品或流体。
     * 两个覆盖层都问的原因见旧版结论：
     *   BookmarkOverlay.getIngredientUnderMouse() 只查“书签 + 查询历史”，不含右侧物品列表
     *   （mezz/jei/gui/overlay/bookmarks/BookmarkOverlay.java:332-341）；
     *   物品列表有自己的命中查询（mezz/jei/gui/overlay/IngredientListOverlay.java:380-389）。
     */
    @Nullable
    public static Hovered getHovered() {
        AEKey key = getBookmarkOverlay()
                .flatMap(IBookmarkOverlay::getIngredientUnderMouse)   // IBookmarkOverlay.java:23
                .map(OrderJeiPlugin::toKey)
                .orElse(null);
        if (key != null) {
            return new Hovered(key, "bookmark");
        }

        key = getIngredientListOverlay()
                .filter(IIngredientListOverlay::isListDisplayed)      // IIngredientListOverlay.java:37
                .flatMap(IIngredientListOverlay::getIngredientUnderMouse)   // :23
                .map(OrderJeiPlugin::toKey)
                .orElse(null);
        return key == null ? null : new Hovered(key, "itemlist");
    }

    /** JEI 元素 -> AE 键；不认识的类型返回 null（例如其它模组的自定义 ingredient）。 */
    @Nullable
    private static AEKey toKey(ITypedIngredient<?> typed) {
        Object ingredient = typed.getIngredient();                   // ITypedIngredient.java:34
        if (ingredient instanceof ItemStack stack) {
            return stack.isEmpty() ? null : AEItemKey.of(stack);     // AEItemKey.java:72
        }
        if (ingredient instanceof FluidStack fluid) {
            return fluid.isEmpty() ? null : AEFluidKey.of(fluid);    // AEFluidKey.java:49
        }
        return null;
    }

    /** 验证用：把缓存状态打成一行日志。 */
    public static void logCacheState() {
        if (bookmarkOverlay != null) {
            Ae2OrderMod.LOGGER.info("[{}] JEI runtime cached", Ae2OrderMod.MOD_ID);
        } else if (isJeiLoaded()) {
            Ae2OrderMod.LOGGER.info("[{}] JEI runtime not found (mod loaded, callback not fired yet)",
                    Ae2OrderMod.MOD_ID);
        } else {
            Ae2OrderMod.LOGGER.info("[{}] JEI runtime not found (jei not loaded)", Ae2OrderMod.MOD_ID);
        }
    }
}
