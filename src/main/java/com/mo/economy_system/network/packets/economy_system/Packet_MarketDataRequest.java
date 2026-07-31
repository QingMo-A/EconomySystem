package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class Packet_MarketDataRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_MarketDataRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_market_data_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_MarketDataRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_MarketDataRequest.encode(packet, buf), Packet_MarketDataRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_MarketDataRequest() {}

    public static void encode(Packet_MarketDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_MarketDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_MarketDataRequest();
    }

    public static void handle(Packet_MarketDataRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                // 获取市场数据
                List<MarketItem> marketItems = MarketManager.getMarketItems();
                // 发送响应数据包
                EconomySystem_NetworkManager.sendToClient(player, new Packet_MarketDataResponse(marketItems));
            }
        });
    }
}


