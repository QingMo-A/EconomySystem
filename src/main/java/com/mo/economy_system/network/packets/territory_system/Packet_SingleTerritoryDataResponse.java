package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.territory_system.Screen_TerritoryBuff;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_SingleTerritoryDataResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SingleTerritoryDataResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_single_territory_data_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SingleTerritoryDataResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SingleTerritoryDataResponse.encode(packet, buf), Packet_SingleTerritoryDataResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final Territory territory;

    public Packet_SingleTerritoryDataResponse(Territory territory) {
        this.territory = territory;
    }

    public static void encode(Packet_SingleTerritoryDataResponse msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.territory.toNBT());
    }

    public static Packet_SingleTerritoryDataResponse decode(FriendlyByteBuf buf) {
        return new Packet_SingleTerritoryDataResponse(Territory.fromNBT(buf.readNbt()));
    }

    public static void handle(Packet_SingleTerritoryDataResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 🔹 处理客户端数据
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof Screen_TerritoryBuff screen) {
                screen.updateTerritory(msg.territory);
            }
        });
    }
}
