package com.mo.economy_system.server.headdisplay;

import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Objects;

/**
 * 客户端玩家头顶头衔渲染器
 */
@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class HeadDisplay {
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Component displayText = buildDisplayText(player);
        if (displayText == null) {
            return;
        }

        event.setCanRender(TriState.TRUE);
        event.setContent(displayText.copy()
                .append(Component.literal(" "))
                .append(event.getOriginalContent().copy()));
    }

    private static Component buildDisplayText(Player player) {
        if (player == null) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
            return null;
        }

        Rank playerRank = PlayerRankManager.getPlayerRankClient(player);
        Title playerTitle = PlayerTitleManager.getPlayerTitleClient(player);
        int playerLevel = PlayerLevelManager.getPlayerLevelClient(player);
        if (playerRank == null || playerTitle == null) {
            return null;
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
            return Component.literal("[" + "Lv" + playerLevel + "] ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("[" + playerTitle.getTitleName() + "]")
                            .withStyle(s -> s.withColor(titleColor)));
        } else {
            // 有特殊rank：等级和rank用rank颜色，称号用称号自己的颜色
            return Component.literal("[" + "Lv" + playerLevel + "] ")
                    .withStyle(rankColorFormatting)
                    .append(Component.literal("[" + playerRank.getRankName() + "] ")
                            .withStyle(rankColorFormatting))
                    .append(Component.literal("[" + playerTitle.getTitleName() + "]")
                            .withStyle(s -> s.withColor(titleColor)));
        }
    }
}
