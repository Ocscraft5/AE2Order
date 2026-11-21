// 自写：客户端“身上带的终端”这一结果的载体。
// source 取值见 OrderCraftPacket.SOURCE_*：SOURCE_INVENTORY 时 slot 是原版背包槽位；
// SOURCE_CURIO 时 slot 是 Curios 下标、curioId 是饰品槽标识（如 "necklace"）。
package com.xiadan.ae2order;

public record HeldTerminal(byte source, int slot, String curioId) {
}
