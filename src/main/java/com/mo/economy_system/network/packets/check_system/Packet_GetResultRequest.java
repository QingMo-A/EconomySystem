package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_GetResultRequest {
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String fileName;
    private final String base64;

    public Packet_GetResultRequest(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String fileName, String base64) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.fileName = fileName;
        this.base64 = base64;
    }

    public static void encode(Packet_GetResultRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.fileName);
        buf.writeUtf(msg.base64);
    }

    public static Packet_GetResultRequest decode(FriendlyByteBuf buf) {
        return new Packet_GetResultRequest(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(Packet_GetResultRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MinecraftServer server = player.server;
            ServerPlayer target = server.getPlayerList().getPlayer(UUID.fromString(msg.senderUUID));
            EconomySystem_NetworkManager.INSTANCE.send(target, new Packet_GetResultResponse(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, msg.base64));
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
