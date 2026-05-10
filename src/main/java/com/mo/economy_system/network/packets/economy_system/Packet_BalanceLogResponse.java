package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.screen.economy_system.logs.Screen_BalanceLog;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class Packet_BalanceLogResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<Packet_BalanceLogResponse> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_balance_log_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_BalanceLogResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_BalanceLogResponse.encode(packet, buf), Packet_BalanceLogResponse::decode);

    private final List<EconomySavedData.BalanceLogEntry> logs;
    private final String category;
    private final int offset;
    private final int limit;
    private final int total;

    public Packet_BalanceLogResponse(List<EconomySavedData.BalanceLogEntry> logs) {
        this(logs, "全部", 0, 50, logs.size());
    }

    public Packet_BalanceLogResponse(List<EconomySavedData.BalanceLogEntry> logs, String category, int offset, int limit, int total) {
        this.logs = logs;
        this.category = category;
        this.offset = offset;
        this.limit = limit;
        this.total = total;
    }

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_BalanceLogResponse msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.category);
        buf.writeInt(msg.offset);
        buf.writeInt(msg.limit);
        buf.writeInt(msg.total);
        buf.writeInt(msg.logs.size());
        for (EconomySavedData.BalanceLogEntry log : msg.logs) {
            buf.writeNbt(log.toNBT());
        }
    }

    public static Packet_BalanceLogResponse decode(FriendlyByteBuf buf) {
        String category = buf.readUtf();
        int offset = buf.readInt();
        int limit = buf.readInt();
        int total = buf.readInt();
        int size = buf.readInt();
        List<EconomySavedData.BalanceLogEntry> logs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                logs.add(EconomySavedData.BalanceLogEntry.fromNBT(tag));
            }
        }
        return new Packet_BalanceLogResponse(logs, category, offset, limit, total);
    }

    public static void handle(Packet_BalanceLogResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof Screen_BalanceLog screen) {
                screen.updateLogs(msg.logs, msg.category, msg.offset, msg.limit, msg.total);
            }
        });
    }
}
