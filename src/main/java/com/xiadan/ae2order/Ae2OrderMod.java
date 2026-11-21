// AE2 Order Helper / AE2 下单助手
// 本模组只使用 AE2 / JEI 的公开 API，不复制、不打包它们的任何代码或资源。
// 许可证见 jar 内 LICENSE 与 THIRD-PARTY-NOTICES.md。
package com.xiadan.ae2order;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Ae2OrderMod.MOD_ID)
public class Ae2OrderMod {

    public static final String MOD_ID = "ae2order";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Ae2OrderMod() {
        LOGGER.info("[{}] mod constructed", MOD_ID);
        Ae2OrderNetwork.register();

        // 自写，原因：确认 mods.toml 里的双语名称（中文用 TOML 的 unicode 转义写入，避免构建期编码问题）
        //   在运行期被 TOML 解析器正确解码。日志里出现“AE2 下单助手”即证明转义正确。
        ModList.get().getModContainerById(MOD_ID).ifPresent(container -> LOGGER.info(
                "[{}] displayName={} version={}",
                MOD_ID,
                container.getModInfo().getDisplayName(),
                container.getModInfo().getVersion()));

        // 软依赖门：JEI 未安装时，书签/物品列表相关逻辑全部静默关闭。
        // 自写，原因（重要）：这里**不能**直接引用 OrderJeiPlugin 的静态方法 —— 那个类 implements
        //   mezz.jei.api.IModPlugin，专用服务器上没有 JEI 的 class，一旦加载它就会
        //   NoClassDefFoundError。所以只查 ModList，并且只打印类名字符串（不触发类加载）。
        if (ModList.get() != null && ModList.get().isLoaded(OrderJeiPlugin.JEI_MOD_ID)) {
            LOGGER.info("[{}] JEI detected, plugin {} is available for annotation scan",
                    MOD_ID, "com.xiadan.ae2order.OrderJeiPlugin");
        } else {
            LOGGER.info("[{}] JEI not loaded, order-from-JEI disabled", MOD_ID);
        }
    }
}
