package com.mo.economy_system.network.packets.economy_system.sales_order;

import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_RemoveSalesOrder {

    private final UUID itemId;

    public Packet_RemoveSalesOrder(UUID itemId) {
        this.itemId = itemId;
    }

    public static void encode(Packet_RemoveSalesOrder msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.itemId);
    }

    public static Packet_RemoveSalesOrder decode(FriendlyByteBuf buf) {
        return new Packet_RemoveSalesOrder(buf.readUUID());
    }

    public static void handle(Packet_RemoveSalesOrder msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 获取市场中的商品
            MarketItem item = MarketManager.getMarketItemById(msg.itemId);
            if (item == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY));
                return;
            }

            System.out.println(!item.getSellerID().equals(player.getUUID()));
            System.out.println(player.hasPermissions(2));
            // 验证是否是卖家
            if (!item.getSellerID().equals(player.getUUID())) {
                if (!Util_Player.isOP(player)) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_UNMATCHED_SELLER_MESSAGE_KEY));
                    return;
                }
            }

            // 从市场中移除商品
            MarketManager.removeMarketItem(item);

            // 将物品返回给卖家
            ItemStack removedItem = item.getItemStack().copy();
            if (!player.getInventory().add(removedItem)) {
                player.drop(removedItem, false);
            }

            // 通知客户端刷新市场界面
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY));
        });
        context.setPacketHandled(true);
    }
}

