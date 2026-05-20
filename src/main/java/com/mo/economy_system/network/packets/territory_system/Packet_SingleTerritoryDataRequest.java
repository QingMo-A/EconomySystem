package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;

import java.util.UUID;

public class Packet_SingleTerritoryDataRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SingleTerritoryDataRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_single_territory_data_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SingleTerritoryDataRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SingleTerritoryDataRequest.encode(packet, buf), Packet_SingleTerritoryDataRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final UUID territoryID;

    public Packet_SingleTerritoryDataRequest(UUID territoryID) {
        this.territoryID = territoryID;
    }

    public static void encode(Packet_SingleTerritoryDataRequest msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
    }

    public static Packet_SingleTerritoryDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_SingleTerritoryDataRequest(buf.readUUID());
    }

    public static void handle(Packet_SingleTerritoryDataRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            // 获取领地数据
            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                System.out.println("领地数据请求失败：该 ID 不存在！");
                return;
            }

            // 发送数据回客户端
            Packet_SingleTerritoryDataResponse response = new Packet_SingleTerritoryDataResponse(territory);
            EconomySystem_NetworkManager.sendToClient(player, response);
        });
    }
}
