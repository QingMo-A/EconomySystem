package com.mo.economy_system.server.serverui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_DeathMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 死亡消息处理器 - 监听玩家死亡事件，发送到右上角显示
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class DeathMessageHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 只处理玩家死亡
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 检查服务器是否存在
        if (player.getServer() == null) return;

        DamageSource damageSource = event.getSource();

        // 获取死亡消息
        Component deathMessage = damageSource.getLocalizedDeathMessage(player);

        // 发送死亡消息给所有在线玩家
        for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> onlinePlayer),
                    new Packet_DeathMessage(deathMessage)
            );
        }
    }
}
