package com.mo.economy_system.network.packets.economy_system.sales_order;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_CreateSalesOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_CreateSalesOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/sales_order/packet_create_sales_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_CreateSalesOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_CreateSalesOrder.encode(packet, buf), Packet_CreateSalesOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final MarketItem marketItem;

    public Packet_CreateSalesOrder(MarketItem marketItem) {
        this.marketItem = marketItem;
    }

    public static void encode(Packet_CreateSalesOrder msg, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(msg.marketItem.toNBT(buf.registryAccess()));
    }

    public static Packet_CreateSalesOrder decode(RegistryFriendlyByteBuf buf) {
        return new Packet_CreateSalesOrder(MarketItem.fromNBT(buf.readNbt(), buf.registryAccess()));
    }

    public static void handle(Packet_CreateSalesOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null; // 获取发送上架请求的玩家

            if (player == null) return;

            EconomySavedData economySavedData = EconomySavedData.getInstance(player.serverLevel());

            // 获取玩家手中的物品
            var heldItem = player.getMainHandItem();

            int price = msg.marketItem.getBasePrice();
            ItemStack listedStack = msg.marketItem.getItemStack();
            if (price <= 0 || listedStack.isEmpty() || listedStack.getCount() <= 0
                    || !msg.marketItem.getSellerID().equals(player.getUUID())
                    || MarketManager.getMarketItemById(msg.marketItem.getTradeID()) != null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY));
                return;
            }

            // 检查是否与上架的物品匹配
            if (!heldItem.isEmpty() && ItemStack.isSameItemSameComponents(heldItem, listedStack)) {
                int requiredAmount = listedStack.getCount();

                if (heldItem.getCount() >= requiredAmount) {
                    int tax = Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(price * 0.1D)));
                    if (!economySavedData.minBalance(player.getUUID(), tax, "税费", "上架商品税: " + listedStack.getHoverName().getString())) {
                        player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY, tax));
                        return;
                    }

                    // 减少背包中的物品
                    heldItem.shrink(requiredAmount);

                    // 将商品加入市场管理器
                    MarketManager.addMarketItem(msg.marketItem);
                    MarketManager.saveTo(player.serverLevel());

                    // 发送成功消息给玩家
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_SUCCESSFULLY_MESSAGE_KEY));
                } else {
                    // 如果物品数量不足，通知玩家
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY));
                }
            } else {
                // 如果手中的物品与上架物品不匹配，通知玩家
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY));
            }
        });
    }

}
