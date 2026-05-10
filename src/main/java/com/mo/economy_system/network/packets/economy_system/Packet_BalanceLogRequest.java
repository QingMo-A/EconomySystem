package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class Packet_BalanceLogRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<Packet_BalanceLogRequest> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_balance_log_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_BalanceLogRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_BalanceLogRequest.encode(packet, buf), Packet_BalanceLogRequest::decode);

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_BalanceLogRequest msg, FriendlyByteBuf buf) {
    }

    public static Packet_BalanceLogRequest decode(FriendlyByteBuf buf) {
        return new Packet_BalanceLogRequest();
    }

    public static void handle(Packet_BalanceLogRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
                EconomySystem_NetworkManager.sendToClient(player, new Packet_BalanceLogResponse(data.getBalanceLogs(player.getUUID())));
            }
        });
    }
}
