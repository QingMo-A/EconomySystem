package com.mo.economy_system.network.packets.economy_system.sales_order;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_CreateSalesOrder {

    private final MarketItem marketItem;

    public Packet_CreateSalesOrder(MarketItem marketItem) {
        this.marketItem = marketItem;
    }

    public static void encode(Packet_CreateSalesOrder msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.marketItem.toNBT());
    }

    public static Packet_CreateSalesOrder decode(FriendlyByteBuf buf) {
        return new Packet_CreateSalesOrder(MarketItem.fromNBT(buf.readNbt()));
    }

    public static void handle(Packet_CreateSalesOrder msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender(); // 获取发送上架请求的玩家

            if (player == null) return;

            EconomySavedData economySavedData = EconomySavedData.getInstance(player.serverLevel());

            // 获取玩家手中的物品
            var heldItem = player.getMainHandItem();

            int tax = (int) (msg.marketItem.getBasePrice() * 0.1);

            if (economySavedData.minBalance(player.getUUID(), tax)) {
                // 检查是否与上架的物品匹配
                if (!heldItem.isEmpty() && ItemStack.isSameItemSameComponents(heldItem, msg.marketItem.getItemStack())) {
                    int requiredAmount = msg.marketItem.getItemStack().getCount();

                    if (heldItem.getCount() >= requiredAmount) {
                        // 减少背包中的物品
                        heldItem.shrink(requiredAmount);

                        // 将商品加入市场管理器
                        MarketManager.addMarketItem(msg.marketItem);

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
            } else {
                // 如果玩家不能支付商品税，通知玩家
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY, tax));
            }
        });
        context.setPacketHandled(true);
    }

}
