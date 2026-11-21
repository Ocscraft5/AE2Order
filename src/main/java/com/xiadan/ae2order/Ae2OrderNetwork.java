// 自写，原因：AE2 的包注册表是硬编码 enum、第三方无法注册
//   （依据 appeng/core/sync/BasePacketHandler.java:58 `public enum PacketTypes`，
//    且 appeng/api 全目录 grep 无任何注册入口），故自建 Forge SimpleChannel 包。
package com.xiadan.ae2order;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Ae2OrderNetwork {

    /** 2 = 支持流体 + “身上带终端即可下单”（包结构相对 1 有变）。 */
    public static final String PROTOCOL = "2";

    public static final ResourceLocation CHANNEL_NAME =
            new ResourceLocation(Ae2OrderMod.MOD_ID, "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

    private static boolean registered = false;

    private Ae2OrderNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        OrderCraftPacket.register(0);
        Ae2OrderMod.LOGGER.info("[{}] network channel registered: {} (1 packet, protocol {})",
                Ae2OrderMod.MOD_ID, CHANNEL_NAME, PROTOCOL);
    }
}
