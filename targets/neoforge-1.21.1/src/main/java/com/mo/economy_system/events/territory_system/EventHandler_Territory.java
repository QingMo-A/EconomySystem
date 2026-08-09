package com.mo.economy_system.events.territory_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.territory.TerritoryRuntimePolicy;
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
        player.sendSystemMessage(Component.translatable(
                        "message.territory.runtime.enter", territory.getName())
                .withStyle(ChatFormatting.GREEN));

        // 设置主标题
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.territory.runtime.welcome", territory.getName())
                        .withStyle(ChatFormatting.AQUA))
        );
        // 设置主标题
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("message.territory.runtime.owner", territory.getOwnerName())
                        .withStyle(ChatFormatting.GOLD))
        );
        // 设置动画效果（淡入、停留、淡出）
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                TerritoryRuntimePolicy.TITLE_FADE_IN_TICKS,
                TerritoryRuntimePolicy.TITLE_STAY_TICKS,
                TerritoryRuntimePolicy.TITLE_FADE_OUT_TICKS));
    }

    @SubscribeEvent
    public static void onPlayerLeaveTerritory(Event_PlayerLeaveTerritory event) {
        ServerPlayer player = event.getPlayer();
        Territory territory = event.getTerritory();

        // 向玩家发送离开领地的消息
        player.sendSystemMessage(Component.translatable(
                        "message.territory.runtime.leave", territory.getName())
                .withStyle(ChatFormatting.RED));
    }
}
