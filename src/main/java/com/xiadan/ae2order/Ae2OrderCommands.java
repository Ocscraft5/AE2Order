// 自写：一个自检命令，用来在“不依赖鼠标点击”的情况下验证服务端链路。
// 作用：用玩家当前的携带状态（开着的终端 或 身上的无线终端）分别对
//   1) 主手里的物品键   2) 流体键（水）
// 调一次 OrderCraftHandler，把结果直接发到聊天栏 + 日志。
// 权限等级 2（OP），因为它会给执行者自己打开 AE2 的下单界面。
package com.xiadan.ae2order;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ae2OrderMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Ae2OrderCommands {

    private Ae2OrderCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("ae2order")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("selftest").executes(Ae2OrderCommands::selftest)));
    }

    private static int selftest(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("[ae2order] selftest must be run by a player"));
            return 0;
        }

        byte source;
        int containerId = -1;
        int slot = -1;
        String curioId = "";

        HeldTerminal held = TerminalFinder.find(player);
        if (held != null) {
            source = held.source();
            slot = held.slot();
            curioId = held.curioId();
        } else if (player.containerMenu instanceof MEStorageMenu) {
            source = OrderCraftPacket.SOURCE_MENU;
            containerId = player.containerMenu.containerId;
        } else {
            player.sendSystemMessage(Component.literal(
                    "[ae2order] selftest: neither an open AE2 terminal nor a carried linked wireless terminal"));
            return 0;
        }

        ItemStack inHand = player.getMainHandItem();
        AEKey itemKey = AEItemKey.of(inHand);
        if (itemKey == null) {
            player.sendSystemMessage(Component.literal("[ae2order] item test skipped: main hand is empty"));
        } else {
            String error = OrderCraftHandler.handle(player, itemKey, source, containerId, slot, curioId);
            player.sendSystemMessage(Component.literal("[ae2order] item test: "
                    + (error == null ? "OK (" + itemKey + ")" : "FAILED (" + error + ")")));
        }

        AEKey fluidKey = AEFluidKey.of(Fluids.WATER);
        String fluidError = OrderCraftHandler.handle(player, fluidKey, source, containerId, slot, curioId);
        player.sendSystemMessage(Component.literal("[ae2order] fluid test: "
                + (fluidError == null ? "OK (" + fluidKey + ")" : "FAILED (" + fluidError + ")")));

        return 1;
    }
}
