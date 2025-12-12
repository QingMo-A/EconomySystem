package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.capability.TitleCapabilityProvider;
import com.mo.economy_system.server.rank.RankRegistry;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

        // 拼接消息  称号+玩家名+文本
        if (RankCapabilityProvider.getPlayerRank(player) == RankRegistry.NO_RANK) {
            customMessage = Component.literal("[" + titleName + "] ")
                    .append(player.getDisplayName())
                    .append(": ")
                    .append(event.getRawText());
        }
        else if (RankCapabilityProvider.getPlayerRank(player) == RankRegistry.FISH) {
            customMessage = Component.literal("§a[" + titleName + "] ")
                    .append(player.getDisplayName()).withStyle(ChatFormatting.GREEN)
                    .append(": ").withStyle(ChatFormatting.GREEN)
                    .append(event.getRawText()).withStyle(ChatFormatting.GREEN);
        }
        else if (RankCapabilityProvider.getPlayerRank(player) == RankRegistry.FISH_PLUS) {
            customMessage = Component.literal("§b[" + titleName + "] ")
                    .append(player.getDisplayName()).withStyle(ChatFormatting.AQUA)
                    .append(": ").withStyle(ChatFormatting.AQUA)
                    .append(event.getRawText()).withStyle(ChatFormatting.AQUA);
        }
        else if  (RankCapabilityProvider.getPlayerRank(player) == RankRegistry.FISH_PLUS_PLUS) {
            customMessage = Component.literal("§6[" + titleName + "] ")
                    .append(player.getDisplayName()).withStyle(ChatFormatting.GOLD)
                    .append(": ").withStyle(ChatFormatting.GOLD)
                    .append(event.getRawText()).withStyle(ChatFormatting.GOLD);;
        }
        else {
            customMessage = Component.literal("[" + titleName + "] ")
                    .append(player.getDisplayName())
                    .append(": ")
                    .append(event.getRawText());
        }


        // true = 仅显示在聊天栏；false = 系统提示（弹提示框）
        for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
            onlinePlayer.sendSystemMessage(customMessage, false);
        }

    }


}
