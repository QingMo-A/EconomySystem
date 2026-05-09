package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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

    public static void encode(Packet_MarketDataResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.items.size());
        for (MarketItem item : msg.items) {
            buf.writeNbt(item.toNBT());
        }
    }

    public static Packet_MarketDataResponse decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MarketItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(MarketItem.fromNBT(buf.readNbt()));
        }
        return new Packet_MarketDataResponse(items);
    }

    public static void handle(Packet_MarketDataResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取当前屏幕实例并更新市场商品
            if (Minecraft.getInstance().screen instanceof Screen_Market screenMarket) {
                screenMarket.updateMarketItems(msg.items);
            }
        });
    }
}

