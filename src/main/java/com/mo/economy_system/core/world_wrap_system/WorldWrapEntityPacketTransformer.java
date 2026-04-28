package com.mo.economy_system.core.world_wrap_system;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WorldWrapEntityPacketTransformer {
    public static Packet<?> transform(Packet<?> packet, ServerPlayer player) {
        if (!WorldWrapEntityMirrorManager.canMirrorEntities(player)) {
            return packet;
        }

        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            return transformBundle(bundlePacket, player);
        }
        if (packet instanceof ClientboundAddEntityPacket addEntityPacket) {
            return transformAddEntity(addEntityPacket, player);
        }
        if (packet instanceof ClientboundTeleportEntityPacket teleportEntityPacket) {
            return transformTeleportEntity(teleportEntityPacket, player);
        }
        return packet;
    }

    @SuppressWarnings("unchecked")
    private static ClientboundBundlePacket transformBundle(ClientboundBundlePacket packet, ServerPlayer player) {
        List<Packet<? super ClientGamePacketListener>> transformedPackets = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> subPacket : packet.subPackets()) {
            Packet<?> transformedPacket = transform(subPacket, player);
            transformedPackets.add((Packet<? super ClientGamePacketListener>) transformedPacket);
            changed = changed || transformedPacket != subPacket;
        }
        return changed ? new ClientboundBundlePacket(transformedPackets) : packet;
    }

    private static ClientboundAddEntityPacket transformAddEntity(ClientboundAddEntityPacket packet, ServerPlayer player) {
        double x = WorldWrapEntityMirrorManager.unwrapXForPlayer(player, packet.getX());
        double z = WorldWrapEntityMirrorManager.unwrapZForPlayer(player, packet.getZ());
        if (x == packet.getX() && z == packet.getZ()) {
            return packet;
        }

        return new ClientboundAddEntityPacket(
                packet.getId(),
                packet.getUUID(),
                x,
                packet.getY(),
                z,
                packet.getXRot(),
                packet.getYRot(),
                packet.getType(),
                packet.getData(),
                new Vec3(packet.getXa(), packet.getYa(), packet.getZa()),
                packet.getYHeadRot()
        );
    }

    private static ClientboundTeleportEntityPacket transformTeleportEntity(ClientboundTeleportEntityPacket packet, ServerPlayer player) {
        double x = WorldWrapEntityMirrorManager.unwrapXForPlayer(player, packet.getX());
        double z = WorldWrapEntityMirrorManager.unwrapZForPlayer(player, packet.getZ());
        if (x == packet.getX() && z == packet.getZ()) {
            return packet;
        }

        return packet;
    }
}
