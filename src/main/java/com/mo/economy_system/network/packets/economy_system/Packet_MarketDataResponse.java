package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class Packet_MarketDataResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_MarketDataResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_market_data_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_MarketDataResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_MarketDataResponse.encode(packet, buf), Packet_MarketDataResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final List<MarketItem> items;

    public Packet_MarketDataResponse(List<MarketItem> items) {
        this.items = items;
    }

    public static void encode(Packet_MarketDataResponse msg, RegistryFriendlyByteBuf buf) {
        buf.writeInt(msg.items.size());
        for (MarketItem item : msg.items) {
            buf.writeNbt(item.toNBT(buf.registryAccess()));
        }
    }

    public static Packet_MarketDataResponse decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MarketItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(MarketItem.fromNBT(buf.readNbt(), buf.registryAccess()));
        }
        return new Packet_MarketDataResponse(items);
    }

    public static void handle(Packet_MarketDataResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取当前屏幕实例并更新市场商品
            if (Minecraft.getInstance().screen instanceof Screen_Market screenMarket) {
                screenMarket.updateMarketItems(msg.items);
            } else if (Minecraft.getInstance().screen instanceof Screen_Home screenHome) {
                int sellCount = 0;
                int buyCount = 0;
                for (MarketItem item : msg.items) {
                    if (item instanceof SalesOrder) {
                        sellCount++;
                    } else if (item instanceof DemandOrder) {
                        buyCount++;
                    }
                }
                screenHome.updateTradeInfo(sellCount, buyCount);
            }
        });
    }
}

