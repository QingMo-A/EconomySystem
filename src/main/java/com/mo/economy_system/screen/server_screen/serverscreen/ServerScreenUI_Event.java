package com.mo.economy_system.screen.server_screen.serverscreen;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerScreenUI_Event {
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (ServerScreenUI.isShowUI() && mc.player != null) {
            if (isVanillaSystemOverlay(event)) {
                event.setCanceled(true); // 仅屏蔽原版系统UI，TipsUI正常渲染
            }
        }
    }

    private static boolean isVanillaSystemOverlay(RenderGuiLayerEvent.Pre event) {
        ResourceLocation[] needHideOverlays = new ResourceLocation[]{
                VanillaGuiLayers.PLAYER_HEALTH,
                VanillaGuiLayers.FOOD_LEVEL,
                VanillaGuiLayers.AIR_LEVEL,
                VanillaGuiLayers.ARMOR_LEVEL,
                VanillaGuiLayers.EXPERIENCE_BAR,
                VanillaGuiLayers.HOTBAR,
                VanillaGuiLayers.CROSSHAIR
        };

        for (ResourceLocation vanillaOverlay : needHideOverlays) {
            if (event.getName().equals(vanillaOverlay)) {
                return true;
            }
        }

        return false;
    }
}
