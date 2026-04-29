package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.core.playerattributes_system.death.DeathEventHandler;
import com.mo.economy_system.core.playerattributes_system.death.DeathItemStorage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 正常复活请求包
 * 客户端点击"正常复活"按钮后发送到服务端
 */
public class Packet_NormalRespawnRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_NormalRespawnRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/death_system/packet_normal_respawn_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_NormalRespawnRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_NormalRespawnRequest.encode(packet, buf), Packet_NormalRespawnRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public Packet_NormalRespawnRequest() {}

    /**
     * 编码（空包，不需要编码）
     */
    public static void encode(Packet_NormalRespawnRequest packet, FriendlyByteBuf buf) {}

    /**
     * 解码
     */
    public static Packet_NormalRespawnRequest decode(FriendlyByteBuf buf) {
        return new Packet_NormalRespawnRequest();
    }

    /**
     * 处理（服务端）
     */
    public static void handle(Packet_NormalRespawnRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
            if (data == null) return;

            float currentRespawnPoint = data.getRespawnPoint();
            boolean isInfected = data.isInfected();

            // 计算正常复活消耗
            float cost = DeathEventHandler.getNormalCost(isInfected);

            // 检查复活点数是否足够
            if (currentRespawnPoint < cost) {
                // 复活点不足，发送失败消息
                sendResponse(player, false, currentRespawnPoint);
                return;
            }

            // 扣除复活点
            data.consumeRespawnPoint(cost);
            PlayerAttributesDataManager.updatePlayerAttributesData(player, data);

            // 掉落存储的物品
            DeathItemStorage.dropStoredItems(player);

            // 清除死亡状态（包括所有持久化的标记）
            DeathEventHandler.clearDeathState(player);

            // 发送成功消息，让客户端执行复活
            sendResponse(player, true, data.getRespawnPoint());

            EconomySystem.LOGGER.info("玩家 {} 正常复活，消耗 {} 复活点（剩余: {}）",
                    player.getScoreboardName(), cost, data.getRespawnPoint());
        });
    }

    /**
     * 发送响应给客户端
     */
    private static void sendResponse(ServerPlayer player, boolean success, float respawnPoint) {
        Packet_NormalRespawnResponse response = new Packet_NormalRespawnResponse(success, respawnPoint);
        EconomySystem_NetworkManager.sendToClient(response, player);
    }
}
