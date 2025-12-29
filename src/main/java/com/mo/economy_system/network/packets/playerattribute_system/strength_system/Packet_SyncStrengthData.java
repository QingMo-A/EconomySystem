package com.mo.economy_system.network.packets.playerattribute_system.strength_system;

import com.mo.economy_system.core.playerattributes_system.strength.StrengthBarRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 体力数据同步包（服务端→客户端）
 * 完全模仿 Packet_JoinMessage 结构，彻底隔离客户端代码
 */
public class Packet_SyncStrengthData {
    private final int currentStrength;
    private final int maxStrength;

    public Packet_SyncStrengthData(int currentStrength, int maxStrength) {
        this.currentStrength = currentStrength;
        this.maxStrength = maxStrength;
    }

    public static void encode(Packet_SyncStrengthData packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.currentStrength);
        buf.writeInt(packet.maxStrength);
    }

    public static Packet_SyncStrengthData decode(FriendlyByteBuf buf) {
        int current = buf.readInt();
        int max = buf.readInt();
        return new Packet_SyncStrengthData(current, max);
    }

    // 对外暴露的handle方法（服务端/客户端都能访问，无客户端类引用）
    public static void handle(Packet_SyncStrengthData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // 保存包数据为final，避免lambda中引用问题
        final int safeCurrentStrength = packet.currentStrength;
        final int safeMaxStrength = packet.maxStrength;

        // 提交任务到主线程，调用隔离的处理方法
        context.enqueueWork(() -> processOnMainThread(safeCurrentStrength, safeMaxStrength));
        context.setPacketHandled(true);
    }

    /**
     * 隔离的主线程处理方法（无客户端类直接引用）
     */
    private static void processOnMainThread(int currentStrength, int maxStrength) {
        // 模仿参考代码：使用safeRunWhenOn，传入客户端专属Runnable
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(currentStrength, maxStrength));
    }

    /**
     * 客户端专属Runnable（@OnlyIn(Dist.CLIENT)标注，服务端不会加载此类）
     * 所有客户端逻辑都放在这里，彻底隔离
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final int currentStrength;
        private final int maxStrength;

        public ClientRunnable(int currentStrength, int maxStrength) {
            this.currentStrength = currentStrength;
            this.maxStrength = maxStrength;
        }

        @Override
        public void run() {
            // 客户端内部逻辑：此处引用客户端类，服务端完全不感知
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;
            // 调用UI类更新缓存
            StrengthBarRenderer.setCurrentStrength(player, this.currentStrength);
            StrengthBarRenderer.setMaxStrength(player, this.maxStrength);
        }
    }

    public int getCurrentStrength() {
        return currentStrength;
    }

    public int getMaxStrength() {
        return maxStrength;
    }
}