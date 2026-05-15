package com.mo.economy_system.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PlayerDollHatItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float GUI_SCALE = 0.62F;
    private static final float GROUND_SCALE = 0.42F;
    private static final float FIXED_SCALE = 0.54F;
    private static final double ITEM_DISPLAY_Y_OFFSET = -0.65D;

    private static PlayerDollHatItemRenderer instance;

    private PlayerModel<net.minecraft.world.entity.player.Player> wideModel;
    private PlayerModel<net.minecraft.world.entity.player.Player> slimModel;

    private PlayerDollHatItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static PlayerDollHatItemRenderer getInstance() {
        if (instance == null) {
            instance = new PlayerDollHatItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlayerDollSkinResolver.ResolvedSkin skin = PlayerDollSkinResolver.resolveSkin(stack, null);
        PlayerModel<net.minecraft.world.entity.player.Player> model = getModel(skin.model());
        PlayerDollHatRenderer.setupSittingPose(model);

        poseStack.pushPose();
        applyDisplayTransform(displayContext, poseStack);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(skin.texture()));
        PlayerDollHatRenderer.renderWithSkinLayers(model, poseStack, vertexConsumer, packedLight);
        poseStack.popPose();
    }

    private void applyDisplayTransform(ItemDisplayContext displayContext, PoseStack poseStack) {
        switch (displayContext) {
            case GUI -> {
                poseStack.translate(0.5D, 1.3D + ITEM_DISPLAY_Y_OFFSET, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
                poseStack.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            }
            case GROUND -> {
                poseStack.translate(0.5D, 0.95D + ITEM_DISPLAY_Y_OFFSET, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.scale(GROUND_SCALE, GROUND_SCALE, GROUND_SCALE);
            }
            case FIXED -> {
                poseStack.translate(0.5D, 1.1D + ITEM_DISPLAY_Y_OFFSET, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(FIXED_SCALE, FIXED_SCALE, FIXED_SCALE);
            }
            default -> {
                poseStack.translate(0.5D, 1.0D + ITEM_DISPLAY_Y_OFFSET, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.36F, 0.36F, 0.36F);
            }
        }
        poseStack.translate(0.0D, -1.5D, 0.0D);
    }

    private PlayerModel<net.minecraft.world.entity.player.Player> getModel(PlayerSkin.Model skinModel) {
        if (wideModel == null || slimModel == null) {
            Minecraft minecraft = Minecraft.getInstance();
            wideModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slimModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        return skinModel == PlayerSkin.Model.SLIM ? slimModel : wideModel;
    }
}
