package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_GetResultRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_GetResultRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "check_system/packet_get_result_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_GetResultRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_GetResultRequest.encode(packet, buf), Packet_GetResultRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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

    public static void handle(Packet_GetResultRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            MinecraftServer server = player.server;
            ServerPlayer target = server.getPlayerList().getPlayer(UUID.fromString(msg.senderUUID));
            EconomySystem_NetworkManager.sendToClient(target, new Packet_GetResultResponse(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, msg.base64));
        });
    }
}
