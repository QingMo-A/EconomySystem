package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_CheckResultRequest {
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String result;

    public Packet_CheckResultRequest(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String result) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.result = result;
    }

    public static void encode(Packet_CheckResultRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.result);
    }

    public static Packet_CheckResultRequest decode(FriendlyByteBuf buf) {
        return new Packet_CheckResultRequest(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(Packet_CheckResultRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MinecraftServer server = player.server;
            ServerPlayer target = server.getPlayerList().getPlayer(UUID.fromString(msg.senderUUID));
            EconomySystem_NetworkManager.INSTANCE.send(target, new Packet_CheckResultResponse(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.result));
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
