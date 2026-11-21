// 自写：在玩家身上找“已绑定网络的无线终端”。
// 覆盖两种携带位置：
//   1) 原版背包（快捷栏 / 主背包 / 护甲 / 副手）—— 客户端与服务端都能读，且槽位可直接交给
//      MenuLocators.forInventorySlot（MenuLocators.java:99）；
//   2) Curios 饰品槽 —— 交给 AE2WTLib 已注册的 CurioLocator（见 compat/CurioSupport）。
// 判定“是无线终端”的依据：appeng/items/tools/powered/WirelessTerminalItem.java:68,144
//   （该类同时是 AE2 无线终端与 AE2WTLib 各终端的父类），getGridKey 非空 = 已绑定到某个 ME 网络。
package com.xiadan.ae2order;

import org.jetbrains.annotations.Nullable;

import com.xiadan.ae2order.compat.CurioSupport;

import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class TerminalFinder {

    /** 原版 Inventory 的槽位总数（36 背包 + 4 护甲 + 1 副手）。 */
    private static final int VANILLA_SLOTS = 41;
    /** 副手固定是第 40 槽。 */
    private static final int OFFHAND_SLOT = 40;

    private TerminalFinder() {
    }

    @Nullable
    public static HeldTerminal find(Player player) {
        var inventory = player.getInventory();

        // 先看“拿在手上”的：主手当前槽 -> 副手 -> 快捷栏 -> 主背包 -> 护甲
        int selected = inventory.selected;
        HeldTerminal found = bySlot(inventory, selected);
        if (found == null) {
            found = bySlot(inventory, OFFHAND_SLOT);
        }
        if (found == null) {
            for (int i = 0; i < 9 && found == null; i++) {
                found = bySlot(inventory, i);
            }
        }
        if (found == null) {
            for (int i = 9; i < 36 && found == null; i++) {
                found = bySlot(inventory, i);
            }
        }
        if (found == null) {
            for (int i = 36; i < 40 && found == null; i++) {
                found = bySlot(inventory, i);
            }
        }
        if (found != null) {
            return found;
        }

        // 再看饰品槽；Curios 未安装时不能触碰 CurioSupport（它引用了 Curios 的类）
        if (ModList.get() != null && ModList.get().isLoaded("curios")) {
            return CurioSupport.findTerminal(player);
        }
        return null;
    }

    @Nullable
    private static HeldTerminal bySlot(net.minecraft.world.entity.player.Inventory inventory, int slot) {
        if (slot < 0 || slot >= VANILLA_SLOTS) {
            return null;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.getItem() instanceof WirelessTerminalItem terminal && terminal.getGridKey(stack).isPresent()) {
            return new HeldTerminal(OrderCraftPacket.SOURCE_INVENTORY, slot, "");
        }
        return null;
    }
}
