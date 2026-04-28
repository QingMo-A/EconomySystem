package com.mo.economy_system.events.territory_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.territory_system.Territory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class EventHandler_Territory {

    @SubscribeEvent
    public static void onPlayerEnterTerritory(Event_PlayerEnterTerritory event) {
        ServerPlayer player = event.getPlayer();
        Territory territory = event.getTerritory();

        // 向玩家发送进入领地的消息
        player.sendSystemMessage(Component.literal("你进入了领地: " + territory.getName())
                .withStyle(ChatFormatting.GREEN));

        // 设置主标题
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("欢迎来到: " + territory.getName()).withStyle(ChatFormatting.AQUA))
        );
        // 设置主标题
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("所有者: " + territory.getOwnerName()).withStyle(ChatFormatting.GOLD))
        );
        // 设置动画效果（淡入、停留、淡出）
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20)); // 10 ticks 淡入，70 ticks 显示，20 ticks 淡出
    }

    @SubscribeEvent
    public static void onPlayerLeaveTerritory(Event_PlayerLeaveTerritory event) {
        ServerPlayer player = event.getPlayer();
        Territory territory = event.getTerritory();

        // 向玩家发送离开领地的消息
        player.sendSystemMessage(Component.literal("你离开了领地: " + territory.getName())
                .withStyle(ChatFormatting.RED));
    }
}

