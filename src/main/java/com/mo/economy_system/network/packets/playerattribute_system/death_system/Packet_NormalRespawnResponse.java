package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.client.cache.ClientCacheManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 正常复活响应包
 * 服务端 → 客户端
 * 通知客户端是否可以执行复活
 */
public class Packet_NormalRespawnResponse {

    private final boolean success;
    private final float respawnPoint;

    public Packet_NormalRespawnResponse(boolean success, float respawnPoint) {
        this.success = success;
        this.respawnPoint = respawnPoint;
    }

    /**
     * 编码
     */
    public static void encode(Packet_NormalRespawnResponse packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeFloat(packet.respawnPoint);
    }

    /**
     * 解码
     */
    public static Packet_NormalRespawnResponse decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        float respawnPoint = buf.readFloat();
        return new Packet_NormalRespawnResponse(success, respawnPoint);
    }

    /**
     * 处理（客户端）
     */
    public static void handle(Packet_NormalRespawnResponse packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleClient(packet);
        });
        context.setPacketHandled(true);
    }

    private static void handleClient(Packet_NormalRespawnResponse packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (packet.success) {
            // 成功，显示消息并执行复活
            mc.player.displayClientMessage(
                    Component.literal("§a复活成功！剩余复活点: " + String.format("%.1f", packet.respawnPoint)),
                    true
            );
            // 同步客户端复活点数
            ClientCacheManager.setRespawnPoint(mc.player.getUUID(), packet.respawnPoint);
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
