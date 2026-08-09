package com.mo.economy_system.target.forge1201.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Forge item-stack renderer for player doll hats. */
public final class Forge1201PlayerDollHatItemRenderer extends BlockEntityWithoutLevelRenderer {
  private static Forge1201PlayerDollHatItemRenderer instance;

  private Forge1201PlayerDollHatItemRenderer() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
  }

  public static Forge1201PlayerDollHatItemRenderer getInstance() {
    if (instance == null) instance = new Forge1201PlayerDollHatItemRenderer();
    return instance;
  }

  @Override
  public void renderByItem(
      ItemStack stack,
      ItemDisplayContext displayContext,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {
    Forge1201CosmeticRenderer.renderDollItem(
        stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
  }
}
