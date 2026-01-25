package com.mo.economy_system.network.packets.playerattribute_system.limb_system;

import com.mo.economy_system.core.playerattributes_system.limb_health_system.LimbClientInjurySync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 肢体受伤同步包（服务端→客户端）
 * 同步玩家受伤部位信息
 */
public class Packet_SyncLimbInjury {
    private final String limbTypeName;  // 受伤部位名称
    private final long injuryTime;      // 受伤时间戳

    public Packet_SyncLimbInjury(String limbTypeName, long injuryTime) {
        this.limbTypeName = limbTypeName;
        this.injuryTime = injuryTime;
    }

    public static void encode(Packet_SyncLimbInjury packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.limbTypeName);
        buf.writeLong(packet.injuryTime);
    }

    public static Packet_SyncLimbInjury decode(FriendlyByteBuf buf) {
        String limbTypeName = buf.readUtf();
        long injuryTime = buf.readLong();
        return new Packet_SyncLimbInjury(limbTypeName, injuryTime);
    }

    public static void handle(Packet_SyncLimbInjury packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final String safeLimbTypeName = packet.limbTypeName;
        final long safeInjuryTime = packet.injuryTime;

        context.enqueueWork(() -> processOnMainThread(safeLimbTypeName, safeInjuryTime));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(String limbTypeName, long injuryTime) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(limbTypeName, injuryTime));
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final String limbTypeName;
        private final long injuryTime;

        public ClientRunnable(String limbTypeName, long injuryTime) {
            this.limbTypeName = limbTypeName;
            this.injuryTime = injuryTime;
        }

        @Override
        public void run() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;

            // 更新客户端受伤数据
            LimbClientInjurySync.recordInjury(player, limbTypeName, injuryTime);
        }
    }

    public String getLimbTypeName() {
        return limbTypeName;
    }

    public long getInjuryTime() {
        return injuryTime;
    }
}
