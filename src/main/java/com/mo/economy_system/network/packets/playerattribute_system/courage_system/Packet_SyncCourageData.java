package com.mo.economy_system.network.packets.playerattribute_system.courage_system;

import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.awt.geom.FlatteningPathIterator;

/**
 * 勇气值数据同步包（服务端→客户端）
 * 完全模仿 Packet_SyncStrengthData 结构，彻底隔离客户端代码
 */
public class Packet_SyncCourageData implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncCourageData> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/courage_system/packet_sync_courage_data"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncCourageData> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncCourageData.encode(packet, buf), Packet_SyncCourageData::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final float currentCourage;
    private final float maxCourage;

    public Packet_SyncCourageData(float currentCourage, float maxCourage) {
        this.currentCourage = currentCourage;
        this.maxCourage = maxCourage;
    }

    public static void encode(Packet_SyncCourageData packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.currentCourage);
        buf.writeFloat(packet.maxCourage);
    }

    public static Packet_SyncCourageData decode(FriendlyByteBuf buf) {
        float current = buf.readFloat();
        float max = buf.readFloat();
        return new Packet_SyncCourageData(current, max);
    }

    // 对外暴露的handle方法（服务端/客户端都能访问，无客户端类引用）
    public static void handle(Packet_SyncCourageData packet, IPayloadContext context) {
        // 保存包数据为final，避免lambda中引用问题
        final float safeCurrentCourage = packet.currentCourage;
        final float safeMaxCourage = packet.maxCourage;

        // 提交任务到主线程，调用隔离的处理方法
        context.enqueueWork(() -> processOnMainThread(safeCurrentCourage, safeMaxCourage));
    }

    /**
     * 隔离的主线程处理方法（无客户端类直接引用）
     */
    private static void processOnMainThread(float currentCourage, float maxCourage) {
        // 模仿参考代码：使用safeRunWhenOn，传入客户端专属Runnable
        new ClientRunnable(currentCourage, maxCourage).run();
    }

    /**
     * 客户端专属Runnable（@OnlyIn(Dist.CLIENT)标注，服务端不会加载此类）
     * 所有客户端逻辑都放在这里，彻底隔离
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements Runnable {
        private final float currentCourage;
        private final float maxCourage;

        public ClientRunnable(float currentCourage, float maxCourage) {
            this.currentCourage = currentCourage;
            this.maxCourage = maxCourage;
        }

        @Override
        public void run() {
            // 客户端内部逻辑：此处引用客户端类，服务端完全不感知
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;
            // 调用勇气值管理类更新客户端缓存（对应你的勇气值管理类）
            PlayerCourageManager.setCurrentCourage(player, this.currentCourage);
            PlayerCourageManager.setMaxCourage(player, this.maxCourage);
        }
    }

    public float getCurrentCourage() {
        return currentCourage;
    }

    public float getMaxCourage() {
        return maxCourage;
    }
}