// 自写：Curios 饰品槽里的终端。单独成类的原因：
//   本类引用了 Curios API 与 AE2WTLib 的 CurioLocator，只有在确认 curios 已加载时才允许加载本类，
//   否则会在未安装 Curios 的环境里抛 NoClassDefFoundError。
// 依据：
//   top/theillusivec4/curios/api/type/util/ICuriosHelper#findFirstCurio(LivingEntity, Predicate)
//     （实测签名：javap -p，curios-forge 1.19.2-5.1.6.4）
//   top/theillusivec4/curios/api/SlotResult#slotContext()/stack()
//   AE2WTLib de/mari_023/ae2wtlib/curio/CurioLocator（公开构造器 (String,int) 与 (SlotContext)）
//   AE2WTLib 在 AE2wtlibForge 的构造器里用 MenuLocators.register(CurioLocator.class, ...) 注册了它
//     （条件 Platform.trinketsPresent() = ModList.isLoaded("curios")），因此它能跨网络序列化，可安全交给
//     CraftAmountMenu.open 使用。
package com.xiadan.ae2order.compat;

import org.jetbrains.annotations.Nullable;

import com.xiadan.ae2order.HeldTerminal;
import com.xiadan.ae2order.OrderCraftPacket;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocator;
import de.mari_023.ae2wtlib.curio.CurioLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

public final class CurioSupport {

    private CurioSupport() {
    }

    @Nullable
    public static HeldTerminal findTerminal(Player player) {
        var result = CuriosApi.getCuriosHelper().findFirstCurio(player, CurioSupport::isLinkedTerminal);
        if (result.isEmpty()) {
            return null;
        }
        SlotResult slotResult = result.get();
        SlotContext context = slotResult.slotContext();
        if (context == null) {
            return null;
        }
        return new HeldTerminal(OrderCraftPacket.SOURCE_CURIO, context.index(), context.identifier());
    }

    @Nullable
    public static MenuLocator locator(String identifier, int index) {
        return new CurioLocator(identifier, index);
    }

    private static boolean isLinkedTerminal(ItemStack stack) {
        return stack.getItem() instanceof WirelessTerminalItem terminal
                && terminal.getGridKey(stack).isPresent();
    }
}
