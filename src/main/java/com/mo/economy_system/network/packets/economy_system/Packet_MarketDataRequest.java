package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class Packet_MarketDataRequest {
    public Packet_MarketDataRequest() {}

    public static void encode(Packet_MarketDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_MarketDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_MarketDataRequest();
    }

    public static void handle(Packet_MarketDataRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 获取市场数据
                List<MarketItem> marketItems = MarketManager.getMarketItems();
                // 发送响应数据包
                EconomySystem_NetworkManager.INSTANCE.send(player, new Packet_MarketDataResponse(marketItems));
            }
        });
        context.setPacketHandled(true);
    }
}


