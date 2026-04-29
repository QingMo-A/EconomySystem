package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_TerritoryDataRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_TerritoryDataRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_territory_data_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_TerritoryDataRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_TerritoryDataRequest.encode(packet, buf), Packet_TerritoryDataRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_TerritoryDataRequest() {}

    public static void encode(Packet_TerritoryDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_TerritoryDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_TerritoryDataRequest();
    }

    public static void handle(Packet_TerritoryDataRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取当前玩家
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {

                var ownedTerritories = TerritoryManager.getTerritoriesByOwner(player.getUUID());
                var authorizedTerritories = TerritoryManager.getAuthorizedTerritories(player.getUUID());

                // 返回领地数据
                EconomySystem_NetworkManager.sendToClient(player, new Packet_TerritoryDataResponse(ownedTerritories, authorizedTerritories));
            }
        });
    }
}
