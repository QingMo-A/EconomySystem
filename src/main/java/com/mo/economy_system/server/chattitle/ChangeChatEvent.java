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
        String playerRank = PlayerRankManager.getPlayerRankServer(player).getRankName();
        int playerLevel = PlayerLevelManager.getPlayerLevelServer(player); // 获取等级
        event.setCanceled(true);

        Component customMessage = null;
        ChatFormatting rankColor = switch (playerRank) {
            case "FISH" -> ChatFormatting.GREEN;
            case "FISH+" -> ChatFormatting.AQUA;
            case "FISH++" -> ChatFormatting.GOLD;
            case "OPERATOR" -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE; // NO_RANK/NULL默认白色
        };

        // 等级前缀
        String levelPrefix = String.format("[Lv.%d] ", playerLevel);

        if (Objects.equals(playerRank, "NO_RANK") || Objects.equals(playerRank, "NULL")) {
            // 无特殊Rank：等级+称号+玩家名+消息（均为默认白色）
            customMessage = Component.literal(levelPrefix)
                    .append(Component.literal("[" + titleName + "] ").withStyle(rankColor))
                    .append(player.getDisplayName())
                    .append(Component.literal(": ").withStyle(rankColor))
                    .append(event.getRawText());
        } else if (playerRank.equals("OPERATOR")) {
            // 管理员等级和Rank为红色，玩家ID和内容为白色
            customMessage = Component.literal(levelPrefix).withStyle(rankColor) // 等级红色
                    .append(Component.literal("[" + playerRank + " | " + titleName + "] ").withStyle(rankColor)) // Rank红色
                    .append(Component.literal(player.getDisplayName().getString()).withStyle(ChatFormatting.WHITE)) // 玩家ID白色
                    .append(Component.literal(": ").withStyle(rankColor)) // 冒号红色
                    .append(Component.literal(event.getRawText()).withStyle(ChatFormatting.WHITE)); // 内容白色
        } else {
            // 其他Rank所有部分均使用对应Rank颜色
            customMessage = Component.literal(levelPrefix).withStyle(rankColor) // 等级
                    .append(Component.literal("[" + playerRank + " | " + titleName + "] ").withStyle(rankColor)) // Rank+称号
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
