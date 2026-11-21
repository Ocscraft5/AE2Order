// 自写：客户端 -> 服务端的“下单”请求包。
// 序列化 API 依据（AE2 12.8.4）：
//   appeng/api/stacks/AEKey.java:90   public static void writeKey(FriendlyByteBuf, AEKey)
//   appeng/api/stacks/AEKey.java:108  public static AEKey readKey(FriendlyByteBuf)
//   AEKey 是物品/流体等“通用键”的公共基类，AEFluidKey 与 AEItemKey 都可直接用它序列化，
//   因此同一个包天然同时支持物品与流体。
package com.xiadan.ae2order;

import java.util.function.Supplier;

import appeng.api.stacks.AEKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class OrderCraftPacket {

    /** 请求来源：AE2 终端界面正开着，用该菜单自己的 locator。 */
    public static final byte SOURCE_MENU = 0;
    /** 请求来源：身上（原版背包 0..40 槽）携带的无线终端。 */
    public static final byte SOURCE_INVENTORY = 1;
    /** 请求来源：Curios 饰品槽里的无线终端。 */
    public static final byte SOURCE_CURIO = 2;

    private final AEKey what;
    private final byte source;
    private final int containerId;
    private final int slot;
    private final String curioId;

    private OrderCraftPacket(AEKey what, byte source, int containerId, int slot, String curioId) {
        this.what = what;
        this.source = source;
        this.containerId = containerId;
        this.slot = slot;
        this.curioId = curioId == null ? "" : curioId;
    }

    /** 终端界面开着：让服务端用 player.containerMenu 的 locator。 */
    public static OrderCraftPacket forMenu(int containerId, AEKey what) {
        return new OrderCraftPacket(what, SOURCE_MENU, containerId, -1, "");
    }

    /** 只是“身上带着终端”：指定原版背包槽位。 */
    public static OrderCraftPacket forInventory(int inventorySlot, AEKey what) {
        return new OrderCraftPacket(what, SOURCE_INVENTORY, -1, inventorySlot, "");
    }

    /** 终端在 Curios 饰品槽：指定槽位标识与下标。 */
    public static OrderCraftPacket forCurio(String curioId, int curioIndex, AEKey what) {
        return new OrderCraftPacket(what, SOURCE_CURIO, -1, curioIndex, curioId);
    }

    public AEKey getWhat() {
        return what;
    }

    public byte getSource() {
        return source;
    }

    public int getContainerId() {
        return containerId;
    }

    public int getSlot() {
        return slot;
    }

    public String getCurioId() {
        return curioId;
    }

    public void encode(FriendlyByteBuf buf) {
        AEKey.writeKey(buf, what);            // AEKey.java:90
        buf.writeByte(source);
        buf.writeVarInt(containerId);
        buf.writeVarInt(slot);
        buf.writeUtf(curioId);
    }

    public static OrderCraftPacket decode(FriendlyByteBuf buf) {
        AEKey what = AEKey.readKey(buf);      // AEKey.java:108
        byte source = buf.readByte();
        int containerId = buf.readVarInt();
        int slot = buf.readVarInt();
        String curioId = buf.readUtf();
        return new OrderCraftPacket(what, source, containerId, slot, curioId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String error = OrderCraftHandler.handle(player, what, source, containerId, slot, curioId);
            if (error != null) {
                // 自写，原因：客户端必须看得到服务端侧被拒的具体原因（排查用），走语言文件便于双语
                player.displayClientMessage(Component.translatable("message.ae2order.rejected", error), false);
            }
        });
        context.setPacketHandled(true);
    }

    /** 在 Ae2OrderNetwork.register() 里调用。 */
    public static void register(int id) {
        Ae2OrderNetwork.CHANNEL
                .messageBuilder(OrderCraftPacket.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OrderCraftPacket::encode)
                .decoder(OrderCraftPacket::decode)
                .consumerMainThread(OrderCraftPacket::handle)
                .add();
    }
}
