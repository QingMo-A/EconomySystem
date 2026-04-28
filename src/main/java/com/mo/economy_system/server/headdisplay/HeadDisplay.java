package com.mo.economy_system.server.headdisplay;

import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.GameRenderer;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Objects;

/**
 * 客户端玩家头顶头衔渲染器
 */
@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class HeadDisplay {
    private static final double RENDER_HEIGHT = 0.8D;
    private static Component displayText = null;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
            return;
        }

        Rank playerRank = PlayerRankManager.getPlayerRankClient(player);
        Title playerTitle = PlayerTitleManager.getPlayerTitleClient(player);
        int playerLevel = PlayerLevelManager.getPlayerLevelClient(player);
        if (playerRank == null || playerTitle == null) {
            return;
        }

        // 获取rank颜色
        ChatFormatting rankColorFormatting = switch (playerRank.getRankName()) {
            case "FISH" -> ChatFormatting.GREEN;
            case "FISH+" -> ChatFormatting.AQUA;
            case "FISH++" -> ChatFormatting.GOLD;
            case "OPERATOR" -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };

        // 获取称号颜色
        int titleColor = playerTitle.getColor();

        // 构建显示文本：等级和rank用rank颜色，称号用称号自己的颜色
        if (Objects.equals(playerRank.getRankName(), RankRegistry.NO_RANK.getRankName()) ||
            Objects.equals(playerRank.getRankName(), RankRegistry.NULL.getRankName())) {
            // 无特殊rank：等级白色，称号自己的颜色
            displayText = Component.literal("[" + "Lv" + playerLevel + "] ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("[" + playerTitle.getTitleName() + "]")
                            .withStyle(s -> s.withColor(titleColor)));
        } else {
            // 有特殊rank：等级和rank用rank颜色，称号用称号自己的颜色
            displayText = Component.literal("[" + "Lv" + playerLevel + "] ")
                    .withStyle(rankColorFormatting)
                    .append(Component.literal("[" + playerRank.getRankName() + "] ")
                            .withStyle(rankColorFormatting))
                    .append(Component.literal("[" + playerTitle.getTitleName() + "]")
                            .withStyle(s -> s.withColor(titleColor)));
        }



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

        PoseStack.Pose pose = poseStack.last();

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
