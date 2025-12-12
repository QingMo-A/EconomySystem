// 文件：com.mo.economy_system.server.events.SyncEvents.java （新建一个类）
package com.mo.economy_system.server.headdisplay;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.ranktitle_system.Packet_SyncRankTitle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class LoginSync {

    // 当一个玩家开始被另一个玩家追踪（进入视野）时触发
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        // event.getTarget() 是被追踪的实体（这里是玩家）
        // event.getEntity() 是开始追踪的玩家（观察者）
        if (event.getTarget() instanceof ServerPlayer targetPlayer) {
            // 向开始追踪的玩家（观察者）发送被追踪玩家的 Rank 和 Title 数据
            EconomySystem_NetworkManager.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
                    new Packet_SyncRankTitle(targetPlayer)
            );
        }
    }
}