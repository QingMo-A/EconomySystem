package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.core.playerattributes_system.death.DeathEventHandler;
import com.mo.economy_system.core.playerattributes_system.death.DeathItemStorage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 死亡不掉落请求包
 * 客户端点击"保留物品"按钮后发送到服务端
 */
public class Packet_KeepInventoryRequest {

    public Packet_KeepInventoryRequest() {}

    /**
     * 编码（空包，不需要编码）
     */
    public static void encode(Packet_KeepInventoryRequest packet, FriendlyByteBuf buf) {}

    /**
     * 解码
     */
    public static Packet_KeepInventoryRequest decode(FriendlyByteBuf buf) {
        return new Packet_KeepInventoryRequest();
    }

    /**
     * 处理（服务端）
     */
    public static void handle(Packet_KeepInventoryRequest packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
            if (data == null) return;

            float currentRespawnPoint = data.getRespawnPoint();
            boolean isInfected = data.isInfected();

            // 计算保留物品消耗（基础消耗 + 30）
            float cost = DeathEventHandler.getKeepInventoryCost(isInfected);

            // 检查复活点数是否足够
            if (currentRespawnPoint < cost) {
                // 复活点不足，发送失败消息
                sendResponse(player, false, currentRespawnPoint);
                return;
            }

            // 扣除复活点
            data.consumeRespawnPoint(cost);
            PlayerAttributesDataManager.updatePlayerAttributesData(player, data);

            // 保留存储的物品（物品已经在玩家身上，不需要额外操作）
            DeathItemStorage.keepStoredItems(player);

            // 清除死亡状态（包括所有持久化的标记）
            DeathEventHandler.clearDeathState(player);

            // 发送成功消息
            sendResponse(player, true, data.getRespawnPoint());

            EconomySystem.LOGGER.info("玩家 {} 消耗 {} 复活点保留物品（剩余: {}）",
                    player.getScoreboardName(), cost, data.getRespawnPoint());
        });
        context.setPacketHandled(true);
    }

    /**
     * 发送响应给客户端
     */
    private static void sendResponse(ServerPlayer player, boolean success, float respawnPoint) {
        Packet_KeepInventoryResponse response = new Packet_KeepInventoryResponse(success, respawnPoint);
        EconomySystem_NetworkManager.sendToClient(response, player);
    }
}
