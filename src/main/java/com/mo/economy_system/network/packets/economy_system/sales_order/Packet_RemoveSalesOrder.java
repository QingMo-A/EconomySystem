package com.mo.economy_system.network.packets.economy_system.sales_order;

import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataResponse;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_RemoveSalesOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RemoveSalesOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/sales_order/packet_remove_sales_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RemoveSalesOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RemoveSalesOrder.encode(packet, buf), Packet_RemoveSalesOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(Packet_RemoveSalesOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            // 获取市场中的商品
            MarketItem item = MarketManager.getMarketItemById(msg.itemId);
            if (!(item instanceof SalesOrder)) {
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
            if (!MarketManager.removeMarketItemById(msg.itemId)) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY));
                return;
            }
            MarketManager.saveTo(player.serverLevel());

            // 将物品返回给卖家
            ItemStack removedItem = item.getItemStack().copy();
            ServerPlayer itemReceiver = player.server.getPlayerList().getPlayer(item.getSellerID());
            if (itemReceiver == null) {
                itemReceiver = player;
            }
            if (!itemReceiver.getInventory().add(removedItem)) {
                itemReceiver.drop(removedItem, false);
            }

            // 通知客户端刷新市场界面
            EconomySystem_NetworkManager.sendToClient(player, new Packet_MarketDataResponse(MarketManager.getMarketItems()));

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY));
        });
    }
}

