package com.mo.economy_system.target.forge1201.client.render;

import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge client event adapter for the shared cosmetic rendering behavior. */
@Mod.EventBusSubscriber(modid = EconomySystemForge1201.MODID, value = Dist.CLIENT)
public final class Forge1201CosmeticRenderEvents {
  private Forge1201CosmeticRenderEvents() {}

  @SubscribeEvent
  public static void renderCosmetics(RenderPlayerEvent.Post event) {
    Forge1201CosmeticRenderer.renderDollOnPlayer(
        event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
        event.getEntity(), event.getPartialTick());
    Forge1201CosmeticRenderer.renderSupporterOnPlayer(
        event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
        event.getEntity(), event.getRenderer().getModel());
  }
}
