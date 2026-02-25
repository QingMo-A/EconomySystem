package com.mo.economy_system.screen.server_screen.serverscreen;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端事件处理器：处理UI状态恢复（如重生后）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerScreenUI_ClientEventHandler {
    private static int tickCounter = 0;

    /**
     * 每秒检查一次UI状态，确保SHOW_UI状态与实际显示一致
     * 处理死亡重生后UI消失的问题
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 每20 tick（1秒）检查一次
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 检查是否在死亡界面或重生界面，如果是则不恢复UI
        if (mc.screen instanceof DeathScreen) {
            // 在死亡/重生界面，不显示TaskUI
            if (ServerScreenUI.isShowUI()) {
                EconomySystem.LOGGER.info("在死亡界面，保持UI开启状态但不显示");
            }
            return;
        }

        // 如果UI应该显示但没有显示（例如重生后）
        // 但如果子屏幕正在显示，则不需要恢复
        if (ServerScreenUI.isShowUI() && !(mc.screen instanceof ServerScreenUI_Screen) && !ServerScreenUI.isSubScreenActive()) {
            EconomySystem.LOGGER.info("检测到UI状态不一致，重新打开TaskUI");
            mc.setScreen(new ServerScreenUI_Screen());
        }
        // 如果UI不应该显示但还在显示
        else if (!ServerScreenUI.isShowUI() && mc.screen instanceof ServerScreenUI_Screen) {
            EconomySystem.LOGGER.info("检测到UI状态不一致，关闭TaskUI");
            mc.setScreen(null);
        }
    }
}
