package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import com.mo.economy_system.server.playerbiomes.PlayerBiomesDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 同步玩家统计数据到客户端（群系探索数 + 解锁蓝图数）
 */
public class Packet_SyncPlayerStats implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncPlayerStats> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerdata_system/packet_sync_player_stats"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncPlayerStats> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncPlayerStats.encode(packet, buf), Packet_SyncPlayerStats::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final int biomesCount;
    private final int blueprintCount;

    public Packet_SyncPlayerStats(int biomesCount, int blueprintCount) {
        this.biomesCount = biomesCount;
        this.blueprintCount = blueprintCount;
    }

    public static void encode(Packet_SyncPlayerStats msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.biomesCount);
        buf.writeInt(msg.blueprintCount);
    }

    public static Packet_SyncPlayerStats decode(FriendlyByteBuf buf) {
        int biomesCount = buf.readInt();
        int blueprintCount = buf.readInt();
        return new Packet_SyncPlayerStats(biomesCount, blueprintCount);
    }

    public static void handle(Packet_SyncPlayerStats msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 同步到ClientCacheManager
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ClientCacheManager.setExploredBiomesCount(mc.player.getUUID(), msg.biomesCount);
                ClientCacheManager.setUnlockedRecipesCount(mc.player.getUUID(), msg.blueprintCount);
            }
        });
    }

    /**
     * 服务端调用：发送玩家的统计数据到客户端
     */
    public static void sendToClient(ServerPlayer player) {
        // 获取已探索群系数量
        int biomesCount = PlayerBiomesDataManager.getExploredBiomeCount(player.getUUID());

        // 获取已解锁蓝图数量（不包括默认解锁的基础物品）
        int blueprintCount = PlayerBlueprintData.getAllUnlockedItems(player).size()
                - PlayerBlueprintData.getDefaultUnlockedItems().size();

        // 发送数据包
        Packet_SyncPlayerStats packet = new Packet_SyncPlayerStats(biomesCount, blueprintCount);
        com.mo.economy_system.network.EconomySystem_NetworkManager.sendToClient(packet, player);
    }
}
