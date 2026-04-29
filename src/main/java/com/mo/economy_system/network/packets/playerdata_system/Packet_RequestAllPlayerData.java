package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class Packet_RequestAllPlayerData implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RequestAllPlayerData> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerdata_system/packet_request_all_player_data"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RequestAllPlayerData> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RequestAllPlayerData.encode(packet, buf), Packet_RequestAllPlayerData::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_RequestAllPlayerData() {}

    // 编码（空包，仅用于触发请求）
    public static void encode(Packet_RequestAllPlayerData msg, FriendlyByteBuf buf) {}

    // 解码
    public static Packet_RequestAllPlayerData decode(FriendlyByteBuf buf) {
        return new Packet_RequestAllPlayerData();
    }

    // 服务端处理：遍历所有玩家，批量发送Packet_SyncPlayerData
    public static void handle(Packet_RequestAllPlayerData msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer requester = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (requester == null) return;

            // 1. 读取全服玩家数据（从你的PlayerDataManager）
            Map<UUID, PlayerData> allPlayerData = PlayerDataManager.loadAllPlayerDataFromFile();

            // 2. 遍历所有玩家，逐个发送Packet_SyncPlayerData给请求的客户端
            for (Map.Entry<UUID, PlayerData> entry : allPlayerData.entrySet()) {
                UUID playerUUID = entry.getKey();
                PlayerData data = entry.getValue();

                // 从PlayerData获取等级/Rank/Title（适配你的PlayerDataManager逻辑）
                ServerPlayer targetPlayer = requester.getServer().getPlayerList().getPlayer(playerUUID);
                if (targetPlayer != null) {
                    // 在线玩家：直接用ServerPlayer构造Packet
                    EconomySystem_NetworkManager.sendToClient(
                            requester,
                            new Packet_SyncPlayerData(targetPlayer)
                    );
                } else {
                    // 离线玩家：手动构造Packet（用PlayerData中的数据）
                    String lastOnlineTime = "离线";
                    if (data.getLastLoginTime() != 0) {
                        // 假设PlayerData有getLastOnlineTime()方法，返回毫秒数
                        long lastOnlineMs = data.getLastLoginTime();
                        lastOnlineTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOnlineMs), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    }
                    Packet_SyncPlayerData offlinePacket = new Packet_SyncPlayerData(
                            playerUUID,
                            data.getPlayerName(),
                            data.getRank().getRankName(),
                            data.getTitle().getTitleName(),
                            data.getLevel(),
                            data.getCurrentExperience(), // 添加经验
                            lastOnlineTime
                    );
                    EconomySystem_NetworkManager.sendToClient(
                            requester,
                            offlinePacket
                    );
                }
            }
            EconomySystem.LOGGER.info("已向玩家{}发送全服{}名玩家的同步数据",
                    requester.getScoreboardName(), allPlayerData.size());
        });
    }
}
