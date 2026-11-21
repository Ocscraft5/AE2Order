// 自写：把“JEI 里指到的那个东西”翻译成 AE2 的服务端公开入口。
// 全部走公开 API（核验自本机解包的 AE2 12.8.4 源码 / javap）：
//   appeng/menu/AEBaseMenu.java:833          public MenuLocator getLocator()
//   appeng/menu/me/common/MEStorageMenu.java:93  public class MEStorageMenu extends AEBaseMenu
//   appeng/menu/me/crafting/CraftAmountMenu.java:86
//                                            public static void open(ServerPlayer, MenuLocator, AEKey, int)
//   appeng/menu/locator/MenuLocators.java:99  public static MenuLocator forInventorySlot(int)
//   appeng/menu/locator/MenuItemLocator.java:24
//        locate() 内部 = player.getInventory().getItem(index) + IMenuItem#getMenuHost
//        => 只要该槽里是实现了 IMenuItem 的无线终端，就能拿到 ITerminalHost 并打开下单界面
//   appeng/items/tools/powered/WirelessTerminalItem.java:68,144,167
//        extends AEBasePoweredItem implements IMenuItem -> getMenuHost 返回 WirelessTerminalMenuHost
//   appeng/helpers/WirelessTerminalMenuHost.java:47
//        implements IPortableTerminal（IPortableTerminal.java:32 extends ITerminalHost）
//   appeng/api/stacks/AEFluidKey.java:44,49  public static AEFluidKey of(Fluid) / of(FluidStack)
// 为什么“只带终端、不开界面”也能下单：
//   CraftAmountMenu.TYPE 由 MenuTypeBuilder.create(CraftAmountMenu::new, ITerminalHost.class) 建立
//   （CraftAmountMenu.java:53），MenuTypeBuilder.open（:151-190）只要求
//   locator.locate(player, ITerminalHost.class) 能拿到 host，并不要求当前开着终端界面。
//   因此用一个指向背包槽的 MenuItemLocator 就能直接打开下单界面，网络由该终端绑定的 ME 网络决定。
package com.xiadan.ae2order;

import org.jetbrains.annotations.Nullable;

import com.xiadan.ae2order.compat.CurioSupport;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ITerminalHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class OrderCraftHandler {

    /** 数量本阶段固定 1。 */
    public static final int FIXED_AMOUNT = 1;

    private OrderCraftHandler() {
    }

    /**
     * 校验并打开 AE2 下单界面。
     *
     * @return null 表示成功；非 null 是给客户端看的拒绝原因（同时落服务端日志）。
     */
    @Nullable
    public static String handle(@Nullable ServerPlayer player, @Nullable AEKey what, byte source,
                                int containerId, int slot, @Nullable String curioId) {
        if (player == null) {
            return reject("player is null (no server context)");
        }
        if (what == null) {
            return reject("key is null");
        }
        // 只接受 AE2 通用的“物品键 / 流体键”；其它键类型（例如自定义 GenericStack）不处理
        if (!(what instanceof AEItemKey) && !(what instanceof AEFluidKey)) {
            return reject("unsupported key type (" + what.getClass().getName() + ")");
        }

        MenuLocator locator;

        if (source == OrderCraftPacket.SOURCE_MENU) {
            // 路径 1：终端界面正开着，沿用该菜单自己的 locator（有线/无线终端都适用）
            if (player.containerMenu == null) {
                return reject("containerMenu is null");
            }
            if (player.containerMenu.containerId != containerId) {
                return reject("containerId mismatch (client=" + containerId
                        + ", server=" + player.containerMenu.containerId + ")");
            }
            if (!(player.containerMenu instanceof MEStorageMenu menu)) {
                return reject("containerMenu is not an AE2 terminal ("
                        + player.containerMenu.getClass().getName() + ")");
            }
            locator = menu.getLocator();                 // AEBaseMenu.java:833
            if (locator == null) {
                return reject("locator is null (G9 FAILED: menu=" + menu.getClass().getName() + ")");
            }

        } else if (source == OrderCraftPacket.SOURCE_INVENTORY) {
            // 路径 2：只要求“人持有终端”——背包 0..40 槽里的无线终端即可
            int size = player.getInventory().getContainerSize();
            if (slot < 0 || slot >= size) {
                return reject("invalid inventory slot " + slot + " (size=" + size + ")");
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof WirelessTerminalItem terminal)) {
                return reject("slot " + slot + " does not hold a wireless terminal (" + stack + ")");
            }
            if (terminal.getGridKey(stack).isEmpty()) {
                return reject("wireless terminal in slot " + slot + " is not linked to a network");
            }
            locator = MenuLocators.forInventorySlot(slot);

        } else if (source == OrderCraftPacket.SOURCE_CURIO) {
            // 路径 3：终端放在 Curios 饰品槽（AE2WTLib 的 CurioLocator 已由它自己注册到 MenuLocators）
            if (curioId == null || curioId.isEmpty() || !ModList.get().isLoaded("curios")) {
                return reject("curio source unavailable (curioId=" + curioId + ")");
            }
            locator = CurioSupport.locator(curioId, slot);
            if (locator.locate(player, ITerminalHost.class) == null) {
                return reject("curio " + curioId + "#" + slot + " does not hold a usable wireless terminal");
            }

        } else {
            return reject("unknown source " + source);
        }

        // 校验通过：调用公开 open 方法（数量固定 1）
        CraftAmountMenu.open(player, locator, what, FIXED_AMOUNT);   // CraftAmountMenu.java:86

        // 自写，原因：open() 返回 void，无法直接判断成败；菜单没换成 CraftAmountMenu
        //   就说明 host 解析失败 / 无 CRAFT 权限 / 终端未绑定或不在范围，必须明确落日志。
        if (!(player.containerMenu instanceof CraftAmountMenu)) {
            return reject("craft amount menu did not open (locator=" + locator + ", upper="
                    + player.containerMenu.getClass().getName() + ")");
        }

        Ae2OrderMod.LOGGER.info("[{}] opened craft amount menu: key={} source={} locator={}",
                Ae2OrderMod.MOD_ID, what, source, locator);
        return null;
    }

    @Nullable
    private static String reject(String reason) {
        Ae2OrderMod.LOGGER.warn("[{}] craft request rejected: {}", Ae2OrderMod.MOD_ID, reason);
        return reason;
    }
}
