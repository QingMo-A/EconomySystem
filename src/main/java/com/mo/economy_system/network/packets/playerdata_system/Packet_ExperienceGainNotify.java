package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.core.playerlevel_system.overalllevel.ExperienceToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 经验获得通知网络包（服务端→客户端）
 * 携带获得的经验值和当前等级进度，用于客户端显示经验Toast
 */
public class Packet_ExperienceGainNotify {
    private final long experienceGained;
    private final int currentLevel;
    private final float progress;

    public Packet_ExperienceGainNotify(long experienceGained, int currentLevel, float progress) {
        this.experienceGained = experienceGained;
        this.currentLevel = currentLevel;
        this.progress = progress;
    }

    public static void encode(Packet_ExperienceGainNotify packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.experienceGained);
        buf.writeInt(packet.currentLevel);
        buf.writeFloat(packet.progress);
    }

    public static Packet_ExperienceGainNotify decode(FriendlyByteBuf buf) {
        long experienceGained = buf.readLong();
        int currentLevel = buf.readInt();
        float progress = buf.readFloat();
        return new Packet_ExperienceGainNotify(experienceGained, currentLevel, progress);
    }

    public static void handle(Packet_ExperienceGainNotify packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ExperienceToast.show(packet.experienceGained, packet.currentLevel, packet.progress);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public long getExperienceGained() {
        return experienceGained;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public float getProgress() {
        return progress;
    }
}