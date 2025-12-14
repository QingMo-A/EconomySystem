package com.mo.economy_system.server.headdisplay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.GameRenderer;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.capability.TitleCapabilityProvider;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

/**
 * 客户端玩家头顶头衔渲染器
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class HeadDisplay {
    private static final double RENDER_HEIGHT = 0.8D;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
            return;
        }

        Rank playerRank = RankCapabilityProvider.getPlayerRank(player);
        Title playerTitle = TitleCapabilityProvider.getPlayerTitle(player);
        if (playerRank == null || playerTitle == null) {
            return;
        }

        ChatFormatting textColorFormatting;
        if (Objects.equals(playerRank.getRankName(), RankRegistry.NO_RANK.getRankName())) {
            textColorFormatting = ChatFormatting.WHITE;
        } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH.getRankName())) {
            textColorFormatting = ChatFormatting.GREEN;
        } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH_PLUS.getRankName())) {
            textColorFormatting = ChatFormatting.AQUA;
        } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH_PLUS_PLUS.getRankName())) {
            textColorFormatting = ChatFormatting.GOLD;
        } else if  (Objects.equals(playerRank.getRankName(), RankRegistry.OPERATOR.getRankName())) {
            textColorFormatting = ChatFormatting.RED;
        } else {
            textColorFormatting = ChatFormatting.WHITE;
        }

        Component displayText = Component.literal("[" + playerRank.getRankName() + "] [" + playerTitle.getTitleName() + "]")
                .withStyle(textColorFormatting);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        PlayerRenderer renderer = event.getRenderer();
        Font font = renderer.getFont();

        poseStack.pushPose();
        poseStack.translate(0.0D, player.getBbHeight() + RENDER_HEIGHT, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        float textWidth = font.width(displayText);
        float halfWidth = textWidth / 2.0F;
        float textHeight = font.lineHeight;

        // 背景参数：半透明黑色（类似原版nametag）
        int backgroundColor = 0x80000000; // 50% 透明黑，可调整透明度（0xA0000000更不透明）
        float paddingX = 4.0F;
        float paddingY = 2.0F;

        // 绘制背景矩形
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float left = -halfWidth - paddingX;
        float right = halfWidth + paddingX;
        float top = -paddingY;
        float bottom = textHeight + paddingY;

        PoseStack.Pose pose = poseStack.last();
        buffer.vertex(pose.pose(), left, bottom, 0.0F).color(backgroundColor >> 24 & 255, backgroundColor >> 16 & 255, backgroundColor >> 8 & 255, backgroundColor & 255).endVertex();
        buffer.vertex(pose.pose(), right, bottom, 0.0F).color(backgroundColor >> 24 & 255, backgroundColor >> 16 & 255, backgroundColor >> 8 & 255, backgroundColor & 255).endVertex();
        buffer.vertex(pose.pose(), right, top, 0.0F).color(backgroundColor >> 24 & 255, backgroundColor >> 16 & 255, backgroundColor >> 8 & 255, backgroundColor & 255).endVertex();
        buffer.vertex(pose.pose(), left, top, 0.0F).color(backgroundColor >> 24 & 255, backgroundColor >> 16 & 255, backgroundColor >> 8 & 255, backgroundColor & 255).endVertex();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend(); // 启用混合以支持透明
        RenderSystem.defaultBlendFunc();
        tesselator.end();

        // 绘制主文本（使用Component样式颜色，带阴影）
        font.drawInBatch(
                displayText,
                -halfWidth,
                0F,
                -1, // 使用Component的颜色
                true,
                pose.pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880 // 全亮光照
        );

        poseStack.popPose();
    }
}