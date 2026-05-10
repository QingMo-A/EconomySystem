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

    public Packet_BalanceLogResponse(List<EconomySavedData.BalanceLogEntry> logs) {
        this.logs = logs;
    }

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_BalanceLogResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.logs.size());
        for (EconomySavedData.BalanceLogEntry log : msg.logs) {
            buf.writeNbt(log.toNBT());
        }
    }

    public static Packet_BalanceLogResponse decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<EconomySavedData.BalanceLogEntry> logs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                logs.add(EconomySavedData.BalanceLogEntry.fromNBT(tag));
            }
        }
        return new Packet_BalanceLogResponse(logs);
    }

    public static void handle(Packet_BalanceLogResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof Screen_BalanceLog screen) {
                screen.updateLogs(msg.logs);
            }
        });
    }
}
