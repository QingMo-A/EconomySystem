package com.mo.economy_system.network.packets.world_wrap_system;

import com.mo.economy_system.client.world_wrap_system.ClientWorldWrapData;
import com.mo.economy_system.core.world_wrap_system.WorldWrapConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_SyncWorldWrapConfig implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncWorldWrapConfig> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "world_wrap_system/packet_sync_world_wrap_config"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncWorldWrapConfig> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncWorldWrapConfig.encode(packet, buf), Packet_SyncWorldWrapConfig::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final boolean enabled;
    private final String dimension;
    private final double centerX;
    private final double centerZ;
    private final double width;
    private final double height;
    private final int cooldownTicks;
    private final double boundaryWarningDistance;

    public Packet_SyncWorldWrapConfig(WorldWrapConfig.WorldWrapConfigData config) {
        this(config.isEnabled(), config.getDimension(), config.getCenterX(), config.getCenterZ(), config.getWidth(),
                config.getHeight(), config.getCooldownTicks(), config.getBoundaryWarningDistance());
    }

    public Packet_SyncWorldWrapConfig(boolean enabled, String dimension, double centerX, double centerZ,
                                      double width, double height, int cooldownTicks, double boundaryWarningDistance) {
        this.enabled = enabled;
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.width = width;
        this.height = height;
        this.cooldownTicks = cooldownTicks;
        this.boundaryWarningDistance = boundaryWarningDistance;
    }

    public static void encode(Packet_SyncWorldWrapConfig packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeUtf(packet.dimension, 256);
        buf.writeDouble(packet.centerX);
        buf.writeDouble(packet.centerZ);
        buf.writeDouble(packet.width);
        buf.writeDouble(packet.height);
        buf.writeVarInt(packet.cooldownTicks);
        buf.writeDouble(packet.boundaryWarningDistance);
    }

    public static Packet_SyncWorldWrapConfig decode(FriendlyByteBuf buf) {
        return new Packet_SyncWorldWrapConfig(
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble()
        );
    }

    public static void handle(Packet_SyncWorldWrapConfig packet, IPayloadContext context) {
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_SyncWorldWrapConfig packet) {
        ClientWorldWrapData.update(packet.enabled, packet.dimension, packet.centerX, packet.centerZ,
                packet.width, packet.height, packet.cooldownTicks, packet.boundaryWarningDistance);
    }
}
