package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_ConfirmDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ConfirmDemandOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/demand_order/packet_confirm_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ConfirmDemandOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ConfirmDemandOrder.encode(packet, buf), Packet_ConfirmDemandOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(Packet_ConfirmDemandOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            // 获取市场中的商品
            MarketItem item = MarketManager.getMarketItemById(msg.itemId);
            if (!(item instanceof DemandOrder demandOrder) || !demandOrder.isDelivered()) {
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
            if (!MarketManager.removeMarketItemById(msg.itemId)) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY));
                return;
            }
            MarketManager.saveTo(player.serverLevel());

            // 将物品返回给卖家
            giveOrDropSplit(player, item.getItemStack());

            // 通知客户端刷新市场界面
            MarketInvalidationBroadcaster.broadcast(player);

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_SUCCESS_KEY, item.getItemStack().getHoverName(), item.getItemStack().getCount()));
        });
    }

    private static void giveOrDropSplit(ServerPlayer player, ItemStack stack) {
        ItemStack template = stack.copy();
        int remaining = Math.max(1, stack.getCount());
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            ItemStack split = template.copy();
            split.setCount(stackSize);
            if (!player.getInventory().add(split)) {
                player.drop(split, false);
            }
            remaining -= stackSize;
        }
    }
}

