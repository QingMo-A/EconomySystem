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

    // 当一个玩家进入另一个玩家的视野里时触发
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer) {
            // 向观察者发送被观察玩家的 Rank 和 Title 数据
            EconomySystem_NetworkManager.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
                    new Packet_SyncRankTitle(targetPlayer)
            );
        }
    }
}