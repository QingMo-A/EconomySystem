package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_RemoveDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RemoveDemandOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/demand_order/packet_remove_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RemoveDemandOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RemoveDemandOrder.encode(packet, buf), Packet_RemoveDemandOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final UUID itemId;

    public Packet_RemoveDemandOrder(UUID itemId) {
        this.itemId = itemId;
    }

    public static void encode(Packet_RemoveDemandOrder msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.itemId);
    }

    public static Packet_RemoveDemandOrder decode(FriendlyByteBuf buf) {
        return new Packet_RemoveDemandOrder(buf.readUUID());
    }

    public static void handle(Packet_RemoveDemandOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            // 获取市场中的商品
            MarketItem item = MarketManager.getMarketItemById(msg.itemId);
            if (item == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY));
                return;
            }

            // 验证是否是卖家
            if (!item.getSellerID().equals(player.getUUID())) {
                if (!Util_Player.isOP(player)) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_UNMATCHED_SELLER_MESSAGE_KEY));
                    return;
                }
            }

            // 从市场中移除商品
            MarketManager.removeMarketItem(item);

            // 将钱返回给卖家
            ServerLevel serverLevel = player.serverLevel();
            EconomySavedData savedData = EconomySavedData.getInstance(serverLevel);

            savedData.addBalance(item.getSellerID(), item.getBasePrice());

            // 通知客户端刷新市场界面
            EconomySystem_NetworkManager.sendToServer(new Packet_MarketDataRequest());

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY));
        });
    }
}

