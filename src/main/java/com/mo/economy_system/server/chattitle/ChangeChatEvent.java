package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.capability.TitleCapabilityProvider;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChangeChatEvent {
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        // 获取发送消息的玩家
        ServerPlayer player = event.getPlayer();

        // 获取玩家的当前称号
        Title playerTitle = TitleCapabilityProvider.getPlayerTitle(player);
        String titleName = playerTitle.getTitleName();
        //取消原版的聊天发送
        event.setCanceled(true);

        Component customMessage = null;
        String player_rank = RankCapabilityProvider.getPlayerRank(player).getRankName();

        if (Objects.equals(player_rank, "NO_RANK") || Objects.equals(player_rank, "NULL")) {
            // 拼接消息  称号+玩家名+文本
            if (player_rank.equals("NO_RANK")) {
                customMessage = Component.literal("[" + titleName + "] ")
                        .append(player.getDisplayName())
                        .append(": ")
                        .append(event.getRawText());
            }
            else {
                customMessage = Component.literal("[" + titleName + "] ")
                        .append(player.getDisplayName())
                        .append(": ")
                        .append(event.getRawText());
            }
        } else {
            // 拼接消息  称号+玩家名+文本
            if (player_rank.equals("FISH")) {
                customMessage = Component.literal("§a[" + player_rank + " - " + titleName + "] ")
                        .append(player.getDisplayName()).withStyle(ChatFormatting.GREEN)
                        .append(": ").withStyle(ChatFormatting.GREEN)
                        .append(event.getRawText()).withStyle(ChatFormatting.GREEN);
            }
            else if (player_rank.equals("FISH+")) {
                customMessage = Component.literal("§b[" + player_rank + " - " + titleName + "] ")
                        .append(player.getDisplayName()).withStyle(ChatFormatting.AQUA)
                        .append(": ").withStyle(ChatFormatting.AQUA)
                        .append(event.getRawText()).withStyle(ChatFormatting.AQUA);
            }
            else if  (player_rank.equals("FISH++")) {
                customMessage = Component.literal("§6[" + player_rank + " - " + titleName + "] ")
                        .append(player.getDisplayName()).withStyle(ChatFormatting.GOLD)
                        .append(": ").withStyle(ChatFormatting.GOLD)
                        .append(event.getRawText()).withStyle(ChatFormatting.GOLD);;
            }
            else if  (player_rank.equals("OPERATOR")) {
                customMessage = Component.literal("§c[" + player_rank + " - " + titleName + "] ")
                        .append(player.getDisplayName()).withStyle(ChatFormatting.GOLD)
                        .append(": ").withStyle(ChatFormatting.GOLD)
                        .append(event.getRawText()).withStyle(ChatFormatting.WHITE);;
            }
            else {
                customMessage = Component.literal("[" + player_rank + " - " + titleName + "] ")
                        .append(player.getDisplayName())
                        .append(": ")
                        .append(event.getRawText());
            }
        }




        // true = 仅显示在聊天栏；false = 系统提示（弹提示框）
        for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
            onlinePlayer.sendSystemMessage(customMessage, false);
        }

    }


}
