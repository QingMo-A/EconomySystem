package com.mo.economy_system.server;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_SyncPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoginSync {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            EconomySystem.LOGGER.debug("非服务端玩家，跳过数据同步");
            return;
        }

        //给新玩家自己发包
        sendSyncPacketToPlayer(newPlayer, newPlayer);
        //广播新玩家数据给所有在线玩家
        broadcastPlayerDataToAllOnlinePlayers(newPlayer);
    }

    //给单个玩家发送指定玩家的同步包
    public static void sendSyncPacketToPlayer(ServerPlayer targetReceiver, ServerPlayer dataOwner) {
        Packet_SyncPlayerData syncPacket = new Packet_SyncPlayerData(dataOwner);
        EconomySystem_NetworkManager.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> targetReceiver),
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

    //当前玩家进服时，主动推送所有已在线玩家的数据给他
    @SubscribeEvent
    public static void onPlayerJoinLevel(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer currentPlayer)) {
            return;
        }

        //获取服务器内所有已在线玩家（除了自己）
        Collection<ServerPlayer> onlinePlayers = currentPlayer.getServer().getPlayerList().getPlayers();
        for (ServerPlayer onlinePlayer : onlinePlayers) {
            if (onlinePlayer.getUUID().equals(currentPlayer.getUUID())) {
                continue;
            }
            //给当前玩家发送每个已在线玩家的数据
            sendSyncPacketToPlayer(currentPlayer, onlinePlayer);
            EconomySystem.LOGGER.info("玩家{}进服，已经向该玩家发送当前所有在线玩家的数据", currentPlayer.getName());
        }
    }
}