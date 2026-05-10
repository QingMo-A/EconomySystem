package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_CreateDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_CreateDemandOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/demand_order/packet_create_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_CreateDemandOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_CreateDemandOrder.encode(packet, buf), Packet_CreateDemandOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static final String LIST_SUCCESSFULLY_MESSAGE_KEY = "message.list.list_successfully";

    private final MarketItem marketItem;

    public Packet_CreateDemandOrder(MarketItem marketItem) {
        this.marketItem = marketItem;
    }

    public static void encode(Packet_CreateDemandOrder msg, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(msg.marketItem.toNBT(buf.registryAccess()));
    }

    public static Packet_CreateDemandOrder decode(RegistryFriendlyByteBuf buf) {
        return new Packet_CreateDemandOrder(MarketItem.fromNBT(buf.readNbt(), buf.registryAccess()));
    }

    public static void handle(Packet_CreateDemandOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null; // 获取发送上架请求的玩家
            if (player == null) return;

            ServerLevel serverLevel = player.serverLevel();
            EconomySavedData savedData = EconomySavedData.getInstance(serverLevel);

            // 验证买家是否有足够货币
            int price = msg.marketItem.getBasePrice();
            if (price <= 0 || msg.marketItem.getItemStack().isEmpty() || msg.marketItem.getItemStack().getCount() <= 0
                    || !msg.marketItem.getSellerID().equals(player.getUUID())
                    || MarketManager.getMarketItemById(msg.marketItem.getTradeID()) != null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY));
                return;
            }
            if (!savedData.hasEnoughBalance(player.getUUID(), price)) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY));
                return;
            }

            if (!savedData.minBalance(player.getUUID(), price, "市场", "创建求购单 " + msg.marketItem.getItemStack().getHoverName().getString())) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY));
                return;
            }
            // 将商品加入市场管理器
            MarketManager.addMarketItem(msg.marketItem);
            MarketManager.saveTo(serverLevel);

            // 发送成功消息给玩家
            player.sendSystemMessage(Component.translatable(LIST_SUCCESSFULLY_MESSAGE_KEY));
        });
    }

}
