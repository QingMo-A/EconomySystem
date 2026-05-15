package com.mo.economy_system.client.render;

import com.mo.economy_system.EconomySystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class SupporterHatRenderEvents {
    @SubscribeEvent
    public static void renderSupporterHat(RenderPlayerEvent.Post event) {
        SupporterHatPlayerLayer.renderMiniPlayer(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getEntity(),
                event.getRenderer().getModel(),
                event.getPartialTick()
        );
    }
}
