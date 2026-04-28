package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.client.cache.ClientCacheManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 死亡不掉落响应包
 * 服务端返回操作结果给客户端
 */
public class Packet_KeepInventoryResponse {

    private final boolean success;
    private final float respawnPoint;

    public Packet_KeepInventoryResponse(boolean success, float respawnPoint) {
        this.success = success;
        this.respawnPoint = respawnPoint;
    }

    /**
     * 编码
     */
    public static void encode(Packet_KeepInventoryResponse packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeFloat(packet.respawnPoint);
    }

    /**
     * 解码
     */
    public static Packet_KeepInventoryResponse decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        float respawnPoint = buf.readFloat();
        return new Packet_KeepInventoryResponse(success, respawnPoint);
    }

    /**
     * 处理（客户端）
     */
    public static void handle(Packet_KeepInventoryResponse packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleClient(packet);
        });
        context.setPacketHandled(true);
    }

    @EventBusSubscriber
    private static class Handler {
        // 静态类用于事件处理
    }

    private static void handleClient(Packet_KeepInventoryResponse packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (packet.success) {
            // 成功：显示消息并重生
            mc.player.displayClientMessage(
                    Component.literal("§a保留物品成功！剩余复活点: " + String.format("%.1f", packet.respawnPoint)),
                    true
            );
            // 同步客户端复活点数
            ClientCacheManager.setRespawnPoint(mc.player.getUUID(), packet.respawnPoint);

            // 重生并关闭屏幕
            mc.player.respawn();
            mc.setScreen(null);
        } else {
            // 失败：显示错误消息
            mc.player.displayClientMessage(
                    Component.literal("§c复活点不足！"),
                    true
            );
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public float getRespawnPoint() {
        return respawnPoint;
    }
}
