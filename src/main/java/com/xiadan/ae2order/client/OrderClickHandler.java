// 自写：全局中键拦截。取代旧版对 MEStorageScreen#mouseClicked 的 Mixin 注入。
// 为什么可以用事件彻底取代 Mixin（本机实测 Forge 1.19.2-43.5.1 字节码确认）：
//   net/minecraft/client/MouseHandler 里先调用 ForgeHooksClient.onMouseButtonPre(III)Z
//   （即 ScreenEvent.MouseButtonPressed.Pre），返回 true（被 cancel）时直接 return，
//   否则才执行 "mouseClicked event handler" 那段 wrapScreenError -> screen.mouseClicked(...)。
//   => 在 Pre 阶段 setCanceled(true) 等价于旧版 cir.setReturnValue(true)，AE2 自带的中键
//      “取物/克隆”逻辑（MEStorageScreen.java:453-468）不会被执行，也不会双触发。
// 为什么 HIGHEST + receiveCanceled：
//   JEI 自己也在 ScreenEvent.MouseButtonPressed.Pre 上处理点击（mezz/jei/forge/startup/
//   EventRegistration.java:60），被它先 cancel 时仍要能拿到事件（receiveCanceled = true），
//   且要排在它前面（HIGHEST），保证“下单”优先于 JEI 的默认点击行为。
package com.xiadan.ae2order.client;

import com.xiadan.ae2order.Ae2OrderMod;
import com.xiadan.ae2order.Ae2OrderNetwork;
import com.xiadan.ae2order.HeldTerminal;
import com.xiadan.ae2order.OrderCraftPacket;
import com.xiadan.ae2order.OrderJeiPlugin;
import com.xiadan.ae2order.TerminalFinder;

import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ae2OrderMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OrderClickHandler {

    private OrderClickHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        // 步骤 1：只认“拾取方块”键（默认鼠标中键；照抄 MEStorageScreen.java:462 的判定方式，兼容玩家改键）
        int button = event.getButton();
        if (button < 0 || !minecraft.options.keyPickItem.matchesMouse(button)) {
            return;
        }

        // 步骤 2：鼠标下必须是 JEI 书签/物品列表里的东西（物品或流体）。
        // 自写，原因（重要）：没装 JEI 的客户端也必须能启动，所以先用 ModList 判一次，
        //   避免真的去加载 OrderJeiPlugin（它 implements mezz.jei.api.IModPlugin）。
        if (ModList.get() == null || !ModList.get().isLoaded(OrderJeiPlugin.JEI_MOD_ID)) {
            return;
        }
        OrderJeiPlugin.Hovered hovered = OrderJeiPlugin.getHovered();
        if (hovered == null) {
            Ae2OrderMod.LOGGER.debug("[{}] middle-click ignored: no JEI ingredient under mouse ({}, {})",
                    Ae2OrderMod.MOD_ID, event.getMouseX(), event.getMouseY());
            return;
        }

        // 步骤 3a：AE2 终端界面正开着 -> 用该菜单的 locator（网络 = 当前终端所在的网络）
        if (player.containerMenu instanceof MEStorageMenu) {
            int containerId = player.containerMenu.containerId;
            Ae2OrderNetwork.CHANNEL.sendToServer(OrderCraftPacket.forMenu(containerId, hovered.key()));
            Ae2OrderMod.LOGGER.info("[{}] order request sent: key={} via=open-terminal containerId={} mouse=({}, {})",
                    Ae2OrderMod.MOD_ID, hovered.key(), containerId, event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
            return;
        }

        // 步骤 3b：只是“身上带着终端” -> 用该终端的 locator（网络 = 终端绑定的网络）
        HeldTerminal held = TerminalFinder.find(player);
        if (held == null) {
            Ae2OrderMod.LOGGER.info("[{}] order skipped: key={} source={} but no linked wireless terminal carried",
                    Ae2OrderMod.MOD_ID, hovered.key(), hovered.source());
            return;
        }

        if (held.source() == OrderCraftPacket.SOURCE_CURIO) {
            Ae2OrderNetwork.CHANNEL.sendToServer(
                    OrderCraftPacket.forCurio(held.curioId(), held.slot(), hovered.key()));
        } else {
            Ae2OrderNetwork.CHANNEL.sendToServer(
                    OrderCraftPacket.forInventory(held.slot(), hovered.key()));
        }
        Ae2OrderMod.LOGGER.info("[{}] order request sent: key={} jei={} via=carried-terminal source={} slot={} curio={}",
                Ae2OrderMod.MOD_ID, hovered.key(), hovered.source(), held.source(), held.slot(), held.curioId());
        event.setCanceled(true);
    }
}
