package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class Packet_BalanceLogRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<Packet_BalanceLogRequest> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_balance_log_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_BalanceLogRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_BalanceLogRequest.encode(packet, buf), Packet_BalanceLogRequest::decode);

    private final String category;
    private final int offset;
    private final int limit;

    public Packet_BalanceLogRequest() {
        this("全部", 0, 50);
    }

    public Packet_BalanceLogRequest(String category, int offset, int limit) {
        this.category = category == null || category.isBlank() ? "全部" : category;
        this.offset = Math.max(0, offset);
        this.limit = Math.max(1, Math.min(100, limit));
    }

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_BalanceLogRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.category);
        buf.writeInt(msg.offset);
        buf.writeInt(msg.limit);
    }

    public static Packet_BalanceLogRequest decode(FriendlyByteBuf buf) {
        return new Packet_BalanceLogRequest(buf.readUtf(), buf.readInt(), buf.readInt());
    }

    public static void handle(Packet_BalanceLogRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
                EconomySavedData.BalanceLogPage page = data.getBalanceLogs(player.getUUID(), msg.category, msg.offset, msg.limit);
                EconomySystem_NetworkManager.sendToClient(player, new Packet_BalanceLogResponse(page.logs(), page.category(), page.offset(), page.limit(), page.total()));
            }
        });
    }
}
