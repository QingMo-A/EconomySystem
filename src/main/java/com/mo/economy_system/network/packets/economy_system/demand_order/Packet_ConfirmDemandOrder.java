package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_ConfirmDemandOrder {

    private final UUID itemId;

    public Packet_ConfirmDemandOrder(UUID itemId) {
        this.itemId = itemId;
    }

    public static void encode(Packet_ConfirmDemandOrder msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.itemId);
    }

    public static Packet_ConfirmDemandOrder decode(FriendlyByteBuf buf) {
        return new Packet_ConfirmDemandOrder(buf.readUUID());
    }

    public static void handle(Packet_ConfirmDemandOrder msg, Supplier<NetworkEvent.Context> contextSupplier) {
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

            // 验证是否是卖家
            if (!item.getSellerID().equals(player.getUUID())) {
                if (!Util_Player.isOP(player)) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_NOT_OWNER_KEY));
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

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_SUCCESS_KEY, item.getItemStack().getHoverName(), item.getItemStack().getCount()));
        });
        context.setPacketHandled(true);
    }
}

