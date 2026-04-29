package com.mo.economy_system.server;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_SyncPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class LoginSync {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        //给新加入玩家发所有在线玩家的数据（包括自己）
        for (ServerPlayer onlinePlayer : newPlayer.getServer().getPlayerList().getPlayers()) {
            sendSyncPacketToPlayer(newPlayer, onlinePlayer);
        }

        //给其他所有在线玩家发新加入玩家的数据
        for (ServerPlayer onlinePlayer : newPlayer.getServer().getPlayerList().getPlayers()) {
            if (!onlinePlayer.getUUID().equals(newPlayer.getUUID())) {
                sendSyncPacketToPlayer(onlinePlayer, newPlayer);
            }
        }
    }


    //给单个玩家发送指定玩家的同步包
    public static void sendSyncPacketToPlayer(ServerPlayer targetReceiver, ServerPlayer dataOwner) {
        Packet_SyncPlayerData syncPacket = new Packet_SyncPlayerData(dataOwner);
        EconomySystem_NetworkManager.sendToClient(
                targetReceiver,
                syncPacket
        );
        EconomySystem.LOGGER.info("已向玩家{}发送{}的同步包",
                targetReceiver.getName().getString(),
                dataOwner.getName().getString()
        );
    }

    //广播指定玩家的数据给所有在线玩家
    public static void broadcastPlayerDataToAllOnlinePlayers(ServerPlayer dataOwner) {
        //获取服务器内所有在线玩家
        Collection<ServerPlayer> onlinePlayers = dataOwner.getServer().getPlayerList().getPlayers();
        for (ServerPlayer onlinePlayer : onlinePlayers) {
            //跳过自己
            if (onlinePlayer.getUUID().equals(dataOwner.getUUID())) {
                continue;
            }
            //给每个在线玩家发送新玩家的数据
            sendSyncPacketToPlayer(onlinePlayer, dataOwner);
        }
    }
}