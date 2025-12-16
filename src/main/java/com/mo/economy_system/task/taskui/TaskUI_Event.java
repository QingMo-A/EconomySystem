package com.mo.economy_system.task.taskui;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class TaskUI_Event {
    @SubscribeEvent
    //一旦显示任务ui，隐藏其他所有的ui
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (TaskUI.isShowUI() && mc.player != null) {
            event.setCanceled(true);
        }
    }
}