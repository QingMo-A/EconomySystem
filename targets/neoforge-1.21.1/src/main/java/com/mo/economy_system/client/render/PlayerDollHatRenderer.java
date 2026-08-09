package com.mo.economy_system.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.item.items.PlayerDollHatItem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public final class PlayerDollHatRenderer {
    private static final float DOLL_SCALE = 0.54F;
    private static final double HEAD_PIVOT_Y_OFFSET = 1.52D;
    private static final double HEAD_TOP_LOCAL_Y_OFFSET = 0.20D;
    private static final double DOLL_Y_OFFSET = 0.0D;
    private static final double DOLL_FORWARD_OFFSET = 0.0D;
    private static final float DOLL_YAW_FIX_DEGREES = 0.0F;
    private static final float LEG_X_ROT = -1.35F;
    private static final float LEG_Y_ROT = 0.32F;
    private static final float LEG_Z_ROT = 0.08F;
    private static final float ARM_X_ROT = -0.22F;
    private static final float ARM_Z_ROT = 0.12F;
    private static final float BODY_X_ROT = 0.08F;
    private static final float BASE_HEAD_SCALE = 1.0F;
    private static final double BASE_HEAD_X_OFFSET = 0.0D / 16.0D;
    private static final double BASE_HEAD_Y_OFFSET = 12.0D / 16.0D;
    private static final double BASE_HEAD_Z_OFFSET = 0.0D / 16.0D;
    private static final float OUTER_LAYER_SCALE = 1.02F;
    private static final float OUTER_HEAD_LAYER_SCALE = 1.0F;
    private static final double HEAD_CENTER_Y = -4.0D / 16.0D;
    private static final double HEAD_OUTER_LAYER_Y_OFFSET = 12.0D / 16.0D;

    private static PlayerModel<Player> wideModel;
    private static PlayerModel<Player> slimModel;

    private PlayerDollHatRenderer() {
    }

    @SubscribeEvent
    public static void renderPlayerDollHat(RenderPlayerEvent.Post event) {
        ItemStack hatStack = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
        if (!(hatStack.getItem() instanceof PlayerDollHatItem)) {
            return;
        }

        PlayerDollSkinResolver.ResolvedSkin resolvedSkin = PlayerDollSkinResolver.resolveSkin(hatStack, event.getEntity());
        PlayerModel<Player> model = getModel(resolvedSkin.model());
        setupSittingPose(model);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        float headYaw = Mth.rotLerp(event.getPartialTick(), event.getEntity().yHeadRotO, event.getEntity().yHeadRot);
        float headPitch = Mth.lerp(event.getPartialTick(), event.getEntity().xRotO, event.getEntity().getXRot());

        poseStack.translate(0.0D, HEAD_PIVOT_Y_OFFSET, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw + DOLL_YAW_FIX_DEGREES));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        poseStack.translate(0.0D, HEAD_TOP_LOCAL_Y_OFFSET + DOLL_Y_OFFSET, DOLL_FORWARD_OFFSET);
        poseStack.scale(DOLL_SCALE, -DOLL_SCALE, -DOLL_SCALE);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(resolvedSkin.texture()));
        renderWithSkinLayers(model, poseStack, vertexConsumer, event.getPackedLight());
        poseStack.popPose();
    }

    private static PlayerModel<Player> getModel(PlayerSkin.Model skinModel) {
        if (wideModel == null || slimModel == null) {
            Minecraft minecraft = Minecraft.getInstance();
            wideModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slimModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        return skinModel == PlayerSkin.Model.SLIM ? slimModel : wideModel;
    }

    static void setupSittingPose(PlayerModel<Player> model) {
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

        model.body.xRot = BODY_X_ROT;
        model.rightArm.xRot = ARM_X_ROT;
        model.leftArm.xRot = ARM_X_ROT;
        model.rightArm.zRot = ARM_Z_ROT;
        model.leftArm.zRot = -ARM_Z_ROT;

        model.rightLeg.xRot = LEG_X_ROT;
        model.leftLeg.xRot = LEG_X_ROT;
        model.rightLeg.yRot = LEG_Y_ROT;
        model.leftLeg.yRot = -LEG_Y_ROT;
        model.rightLeg.zRot = LEG_Z_ROT;
        model.leftLeg.zRot = -LEG_Z_ROT;

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

    static void renderWithSkinLayers(PlayerModel<Player> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight) {
        setBaseLayerVisible(model, true);
        model.head.visible = false;
        setOuterLayerVisible(model, false);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        setBaseLayerVisible(model, false);
        model.head.visible = true;
        poseStack.pushPose();
        poseStack.translate(BASE_HEAD_X_OFFSET, BASE_HEAD_Y_OFFSET, BASE_HEAD_Z_OFFSET);
        poseStack.translate(0.0D, HEAD_CENTER_Y, 0.0D);
        poseStack.scale(BASE_HEAD_SCALE, BASE_HEAD_SCALE, BASE_HEAD_SCALE);
        poseStack.translate(0.0D, -HEAD_CENTER_Y, 0.0D);
        model.head.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();

        setBaseLayerVisible(model, false);
        setOuterLayerVisible(model, true);
        model.hat.visible = false;
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.scale(OUTER_LAYER_SCALE, OUTER_LAYER_SCALE, OUTER_LAYER_SCALE);
        poseStack.translate(0.0D, -1.5D, 0.0D);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();

        setOuterLayerVisible(model, false);
        model.hat.visible = true;
        poseStack.pushPose();
        poseStack.translate(0.0D, HEAD_OUTER_LAYER_Y_OFFSET, 0.0D);
        poseStack.translate(0.0D, HEAD_CENTER_Y, 0.0D);
        poseStack.scale(OUTER_HEAD_LAYER_SCALE, OUTER_HEAD_LAYER_SCALE, OUTER_HEAD_LAYER_SCALE);
        poseStack.translate(0.0D, -HEAD_CENTER_Y, 0.0D);
        model.hat.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
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
