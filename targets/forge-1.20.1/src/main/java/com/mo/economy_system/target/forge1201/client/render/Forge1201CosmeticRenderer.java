package com.mo.economy_system.target.forge1201.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mo.economy_system.target.forge1201.item.Forge1201PlayerDollHatItem;
import com.mo.economy_system.target.forge1201.item.Forge1201SupporterHat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Shared Forge-side drawing routines for the dynamic cosmetic items. */
final class Forge1201CosmeticRenderer {
  private static final float DOLL_SCALE = 0.54F;
  private static final float MINI_SCALE = 0.28F;
  private static final float GUI_SCALE = 0.62F;
  private static final float GROUND_SCALE = 0.42F;
  private static final float FIXED_SCALE = 0.54F;
  private static final double HEAD_PIVOT_Y_OFFSET = 1.52D;
  private static final double HEAD_TOP_LOCAL_Y_OFFSET = 0.20D;

  private static PlayerModel<Player> wideModel;
  private static PlayerModel<Player> slimModel;

  private Forge1201CosmeticRenderer() {}

  static void renderDollItem(
      ItemStack stack,
      ItemDisplayContext displayContext,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {
    Forge1201PlayerSkinResolver.ResolvedSkin skin =
        Forge1201PlayerSkinResolver.resolveDoll(stack, null);
    PlayerModel<Player> model = getModel(skin.slim());
    setupDollPose(model);
    poseStack.pushPose();
    applyDisplayTransform(displayContext, poseStack);
    VertexConsumer vertexConsumer =
        bufferSource.getBuffer(RenderType.entityTranslucent(skin.texture()));
    renderWithSkinLayers(model, poseStack, vertexConsumer, packedLight);
    poseStack.popPose();
  }

  static void renderDollOnPlayer(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      Player player,
      float partialTick) {
    ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
    if (!(stack.getItem() instanceof Forge1201PlayerDollHatItem)) return;
    Forge1201PlayerSkinResolver.ResolvedSkin skin =
        Forge1201PlayerSkinResolver.resolveDoll(stack, player);
    PlayerModel<Player> model = getModel(skin.slim());
    setupDollPose(model);
    poseStack.pushPose();
    float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
    float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
    poseStack.translate(0.0D, HEAD_PIVOT_Y_OFFSET, 0.0D);
    poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
    poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
    poseStack.translate(0.0D, HEAD_TOP_LOCAL_Y_OFFSET, 0.0D);
    poseStack.scale(DOLL_SCALE, -DOLL_SCALE, -DOLL_SCALE);
    poseStack.translate(0.0D, -1.5D, 0.0D);
    VertexConsumer vertexConsumer =
        bufferSource.getBuffer(RenderType.entityTranslucent(skin.texture()));
    renderWithSkinLayers(model, poseStack, vertexConsumer, packedLight);
    poseStack.popPose();
  }

  static void renderSupporterOnPlayer(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      Player player,
      PlayerModel<?> parentModel) {
    ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
    if (!(helmet.getItem() instanceof Forge1201SupporterHat)) return;
    Forge1201PlayerSkinResolver.ResolvedSkin skin =
        Forge1201PlayerSkinResolver.resolveSupporter(helmet, player);
    PlayerModel<Player> model = getModel(skin.slim());
    setupMiniPose(model);
    poseStack.pushPose();
    parentModel.head.translateAndRotate(poseStack);
    poseStack.translate(0.0D, -0.92D, 0.0D);
    poseStack.scale(MINI_SCALE, -MINI_SCALE, -MINI_SCALE);
    poseStack.translate(0.0D, -1.5D, 0.0D);
    VertexConsumer vertexConsumer =
        bufferSource.getBuffer(RenderType.entityTranslucent(skin.texture()));
    model.renderToBuffer(
        poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
        1.0F, 1.0F, 1.0F, 1.0F);
    poseStack.popPose();
  }

  private static PlayerModel<Player> getModel(boolean slim) {
    if (wideModel == null || slimModel == null) {
      Minecraft minecraft = Minecraft.getInstance();
      wideModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      slimModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    return slim ? slimModel : wideModel;
  }

  private static void applyDisplayTransform(ItemDisplayContext context, PoseStack poseStack) {
    switch (context) {
      case GUI -> {
        poseStack.translate(0.5D, 0.05D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
        poseStack.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
      }
      case GROUND -> {
        poseStack.translate(0.5D, 0.30D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(GROUND_SCALE, GROUND_SCALE, GROUND_SCALE);
      }
      case FIXED -> {
        poseStack.translate(0.5D, 0.10D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(FIXED_SCALE, FIXED_SCALE, FIXED_SCALE);
      }
      default -> {
        poseStack.translate(0.5D, 0.35D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.36F, 0.36F, 0.36F);
      }
    }
    poseStack.translate(0.0D, -1.5D, 0.0D);
  }

  private static void setupDollPose(PlayerModel<Player> model) {
    model.setAllVisible(true);
    model.head.resetPose();
    model.hat.resetPose();
    model.body.resetPose();
    model.rightArm.resetPose();
    model.leftArm.resetPose();
    model.rightLeg.resetPose();
    model.leftLeg.resetPose();
    model.leftSleeve.resetPose();
    model.rightSleeve.resetPose();
    model.leftPants.resetPose();
    model.rightPants.resetPose();
    model.jacket.resetPose();
    model.body.xRot = 0.08F;
    model.rightArm.xRot = -0.22F;
    model.leftArm.xRot = -0.22F;
    model.rightArm.zRot = 0.12F;
    model.leftArm.zRot = -0.12F;
    model.rightLeg.xRot = -1.35F;
    model.leftLeg.xRot = -1.35F;
    model.rightLeg.yRot = 0.32F;
    model.leftLeg.yRot = -0.32F;
    model.rightLeg.zRot = 0.08F;
    model.leftLeg.zRot = -0.08F;
    model.hat.copyFrom(model.head);
    model.rightSleeve.copyFrom(model.rightArm);
    model.leftSleeve.copyFrom(model.leftArm);
    model.rightPants.copyFrom(model.rightLeg);
    model.leftPants.copyFrom(model.leftLeg);
    model.jacket.copyFrom(model.body);
    model.body.offsetPos(new Vector3f(0.0F, 1.0F, 0.0F));
    model.jacket.offsetPos(new Vector3f(0.0F, 1.0F, 0.0F));
    model.rightLeg.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
    model.leftLeg.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
    model.rightPants.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
    model.leftPants.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
  }

  private static void setupMiniPose(PlayerModel<Player> model) {
    setupDollPose(model);
    model.body.xRot = 0.0F;
    model.rightArm.xRot = -0.35F;
    model.leftArm.xRot = -0.35F;
    model.rightArm.zRot = 0.18F;
    model.leftArm.zRot = -0.18F;
    model.rightLeg.xRot = -1.35F;
    model.leftLeg.xRot = -1.35F;
  }

  private static void renderWithSkinLayers(
      PlayerModel<Player> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight) {
    setBaseLayerVisible(model, true);
    model.head.visible = false;
    setOuterLayerVisible(model, false);
    model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
        1.0F, 1.0F, 1.0F, 1.0F);
    setBaseLayerVisible(model, false);
    model.head.visible = true;
    poseStack.pushPose();
    poseStack.translate(0.0D, 0.75D, 0.0D);
    model.head.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
    setBaseLayerVisible(model, false);
    setOuterLayerVisible(model, true);
    model.hat.visible = false;
    poseStack.pushPose();
    poseStack.translate(0.0D, 1.5D, 0.0D);
    poseStack.scale(1.02F, 1.02F, 1.02F);
    poseStack.translate(0.0D, -1.5D, 0.0D);
    model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
        1.0F, 1.0F, 1.0F, 1.0F);
    poseStack.popPose();
    setOuterLayerVisible(model, false);
    model.hat.visible = true;
    poseStack.pushPose();
    poseStack.translate(0.0D, 0.75D, 0.0D);
    model.hat.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
    setBaseLayerVisible(model, true);
    setOuterLayerVisible(model, true);
  }

  private static void setBaseLayerVisible(PlayerModel<Player> model, boolean visible) {
    model.head.visible = visible;
    model.body.visible = visible;
    model.rightArm.visible = visible;
    model.leftArm.visible = visible;
    model.rightLeg.visible = visible;
    model.leftLeg.visible = visible;
  }

  private static void setOuterLayerVisible(PlayerModel<Player> model, boolean visible) {
    model.hat.visible = visible;
    model.jacket.visible = visible;
    model.rightSleeve.visible = visible;
    model.leftSleeve.visible = visible;
    model.rightPants.visible = visible;
    model.leftPants.visible = visible;
  }
}
