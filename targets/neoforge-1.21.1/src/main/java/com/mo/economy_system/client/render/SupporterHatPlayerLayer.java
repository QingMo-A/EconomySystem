package com.mo.economy_system.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mo.economy_system.armor.armors.SupporterHat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SupporterHatPlayerLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    private static final float MINI_SCALE = 0.28F;
    private static final Map<UUID, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();

    private final PlayerModel<T> wideModel;
    private final PlayerModel<T> slimModel;

    public SupporterHatPlayerLayer(RenderLayerParent<T, PlayerModel<T>> renderer) {
        super(renderer);
        Minecraft minecraft = Minecraft.getInstance();
        this.wideModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        renderMiniPlayer(poseStack, bufferSource, packedLight, player, getParentModel(), netHeadYaw, headPitch);
    }

    public static void renderMiniPlayer(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Player player, PlayerModel<?> parentModel, float partialTick) {
        RENDER_HELPER.renderMiniPlayer(poseStack, bufferSource, packedLight, player, parentModel, 0.0F, 0.0F);
    }

    private static final SupporterHatPlayerLayer<Player> RENDER_HELPER = new SupporterHatPlayerLayer<>(new RenderLayerParent<>() {
        private final PlayerModel<Player> model = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);

        @Override
        public PlayerModel<Player> getModel() {
            return model;
        }

        @Override
        public net.minecraft.resources.ResourceLocation getTextureLocation(Player entity) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkin(entity.getGameProfile()).texture();
        }
    });

    private void renderMiniPlayer(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Player player, PlayerModel<?> parentModel, float netHeadYaw, float headPitch) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof SupporterHat)) {
            return;
        }

        Optional<UUID> supporterUuid = SupporterHat.getSupporterUuid(helmet);
        PlayerSkin skin = resolveSkin(supporterUuid.orElse(player.getUUID()), player);
        PlayerModel<T> model = skin.model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
        setupSittingModel(model, netHeadYaw, headPitch);

        poseStack.pushPose();
        parentModel.head.translateAndRotate(poseStack);
        poseStack.translate(0.0D, -0.92D, 0.0D);
        poseStack.scale(MINI_SCALE, -MINI_SCALE, -MINI_SCALE);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(skin.texture()));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    private PlayerSkin resolveSkin(UUID uuid, Player fallbackPlayer) {
        PlayerSkin cached = SKIN_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = new GameProfile(uuid, fallbackPlayer.getGameProfile().getName());
        PlayerSkin immediate = minecraft.getSkinManager().getInsecureSkin(profile);
        SKIN_CACHE.put(uuid, immediate);
        minecraft.getSkinManager().getOrLoad(profile).thenAccept(skin -> SKIN_CACHE.put(uuid, skin));
        return immediate;
    }

    private void setupSittingModel(PlayerModel<T> model, float netHeadYaw, float headPitch) {
        model.setAllVisible(true);
        model.getHead().resetPose();
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

        model.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        model.head.xRot = headPitch * ((float) Math.PI / 180F);
        model.hat.copyFrom(model.head);

        model.body.xRot = 0.0F;
        model.rightArm.xRot = -0.35F;
        model.leftArm.xRot = -0.35F;
        model.rightArm.zRot = 0.18F;
        model.leftArm.zRot = -0.18F;

        model.rightLeg.xRot = -1.35F;
        model.leftLeg.xRot = -1.35F;
        model.rightLeg.yRot = 0.32F;
        model.leftLeg.yRot = -0.32F;
        model.rightLeg.zRot = 0.08F;
        model.leftLeg.zRot = -0.08F;

        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.jacket.copyFrom(model.body);

        model.body.offsetPos(new Vector3f(0.0F, 1.0F, 0.0F));
        model.rightLeg.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
        model.leftLeg.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
        model.rightPants.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
        model.leftPants.offsetPos(new Vector3f(0.0F, 1.0F, 2.0F));
    }
}
