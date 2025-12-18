package com.mo.economy_system.core.task_system.taskui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.task_system.TaskDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

//调用事件，隐藏或者显示按钮


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