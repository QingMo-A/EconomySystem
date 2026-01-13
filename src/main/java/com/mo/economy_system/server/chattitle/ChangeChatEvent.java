package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChangeChatEvent {
    // 在ChangeChatEvent的onPlayerChat方法中修改
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Title playerTitle = PlayerTitleManager.getPlayerTitleServer(player);
        String titleName = playerTitle.getTitleName();
        int titleColor = playerTitle.getColor(); // 称号自己的颜色（RGB格式）
        String playerRank = PlayerRankManager.getPlayerRankServer(player).getRankName();
        int playerLevel = PlayerLevelManager.getPlayerLevelServer(player); // 获取等级

        // 调试日志
        EconomySystem.LOGGER.info("聊天格式化 - 玩家:{}, 称号:{}, 称号颜色:0x{}, Rank:{}", player.getScoreboardName(), titleName, Integer.toHexString(titleColor), playerRank);

        event.setCanceled(true);

        Component customMessage = null;
        ChatFormatting rankColor = switch (playerRank) {
            case "FISH" -> ChatFormatting.GREEN;
            case "FISH+" -> ChatFormatting.AQUA;
            case "FISH++" -> ChatFormatting.GOLD;
            case "OPERATOR" -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY; // NO_RANK/NULL默认白色
        };

        // 等级前缀
        String levelPrefix = String.format("[Lv.%d] ", playerLevel);

        if (playerRank.equals("NO_RANK") || playerRank.equals("NULL")) {
            // 无特殊Rank：等级白色，称号使用自己的颜色
            customMessage = Component.literal(levelPrefix)
                    .append(Component.literal("[" + titleName + "] ").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(titleColor))))
                    .append(player.getDisplayName())
                    .append(Component.literal(": "))
                    .append(event.getRawText());
        } else if (playerRank.equals("OPERATOR")) {
            // 管理员：等级红色，Rank红色，称号自己的颜色，玩家ID和内容白色
            customMessage = Component.literal(levelPrefix).withStyle(rankColor) // 等级红色
                    .append(Component.literal("[" + playerRank + "] ").withStyle(rankColor)) // Rank红色
                    .append(Component.literal("[" + titleName + "] ").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(titleColor)))) // 称号自己的颜色
                    .append(Component.literal(player.getDisplayName().getString()).withStyle(ChatFormatting.WHITE)) // 玩家ID白色
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(event.getRawText()).withStyle(ChatFormatting.WHITE)); // 内容白色
        } else {
            // 其他Rank：等级使用Rank颜色，Rank使用Rank颜色，称号使用自己的颜色
            customMessage = Component.literal(levelPrefix).withStyle(rankColor) // 等级
                    .append(Component.literal("[" + playerRank + "] ").withStyle(rankColor)) // Rank
                    .append(Component.literal("[" + titleName + "] ").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(titleColor)))) // 称号自己的颜色
                    .append(Component.literal(player.getDisplayName().getString()).withStyle(rankColor))// 玩家ID
                    .append(Component.literal(": ").withStyle(rankColor)) // 冒号
                    .append(Component.literal(event.getRawText()).withStyle(rankColor)); // 消息内容
        }

        // 发送格式化消息
        for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
            onlinePlayer.sendSystemMessage(customMessage, false);
        }
    }


}
