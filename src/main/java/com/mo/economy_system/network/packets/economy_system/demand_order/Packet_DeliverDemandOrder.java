package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataResponse;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_DeliverDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_DeliverDemandOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/demand_order/packet_deliver_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_DeliverDemandOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_DeliverDemandOrder.encode(packet, buf), Packet_DeliverDemandOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final UUID itemId; // 商品的唯一 ID

    public Packet_DeliverDemandOrder(UUID itemId) {
        this.itemId = itemId;
    }

    public static void encode(Packet_DeliverDemandOrder msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.itemId);
    }

    public static Packet_DeliverDemandOrder decode(FriendlyByteBuf buf) {
        return new Packet_DeliverDemandOrder(buf.readUUID());
    }

    public static void handle(Packet_DeliverDemandOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer supplier = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (supplier == null) return;

            ServerLevel serverLevel = supplier.serverLevel();
            EconomySavedData savedData = EconomySavedData.getInstance(serverLevel);

            // 查找市场中的商品
            MarketItem item = MarketManager.getMarketItemById(msg.itemId);
            if (!(item instanceof DemandOrder demandOrder) || demandOrder.isDelivered()) {
                supplier.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_ITEM_DOES_NOT_EXIST_MESSAGE_KEY));
                return;
            }

            // 验证买家是否有足够资源
            // 检测并移除物品
            int price = item.getBasePrice();
            if (price <= 0 || item.getSellerID().equals(supplier.getUUID())) {
                supplier.sendSystemMessage(Component.translatable(Util_MessageKeys.DELIVERY_NOT_ENOUGH_ITEMS_KEY));
                return;
            }
            ItemStack requestedStack = item.getItemStack();
            if (consumeItem(supplier, requestedStack, requestedStack.getCount())) {
                // 扣除供货者资源并将货币发放给供货者
                savedData.addBalance(supplier.getUUID(), price);

                demandOrder.setDelivered(true);
                // 通知供货者成功交付
                supplier.sendSystemMessage(Component.translatable(Util_MessageKeys.DELIVERY_SUCCESS_KEY, requestedStack.getHoverName().getString(), requestedStack.getCount()));
                MarketManager.saveTo(serverLevel);

                UUID requesterID = item.getSellerID();
                // 通知求购者（如果在线）
                ServerPlayer requester = serverLevel.getServer().getPlayerList().getPlayer(requesterID);
                if (requester != null) {
                    requester.sendSystemMessage(Component.translatable(Util_MessageKeys.ORDER_DELIVERED_BY_PLAYER_KEY, requestedStack.getHoverName().getString(), requestedStack.getCount(), supplier.getName().getString()));
                } else {
                    String text = Component.translatable(Util_MessageKeys.ORDER_DELIVERED_BY_PLAYER_KEY, requestedStack.getHoverName().getString(), requestedStack.getCount(), supplier.getName().getString()).getString();
                    savedData.storeOfflineMessage(requesterID, text);
                }
            } else {
                supplier.sendSystemMessage(Component.translatable(Util_MessageKeys.DELIVERY_NOT_ENOUGH_ITEMS_KEY));
            }

            // 通知客户端刷新市场界面
            EconomySystem_NetworkManager.sendToClient(supplier, new Packet_MarketDataResponse(MarketManager.getMarketItems()));
        });
    }

    /**
     * 检测玩家是否有指定数量的指定物品，并移除这些物品。
     *
     * @param player       玩家
     * @param targetStack  目标物品堆栈（用于匹配物品类型）
     * @param requiredCount 需要的数量
     * @return 如果玩家有足够数量的物品并成功移除，返回 true；否则返回 false
     */
    public static boolean consumeItem(ServerPlayer player, ItemStack targetStack, int requiredCount) {
        // 检查玩家是否有足够数量的物品
        int totalCount = getItemCount(player, targetStack);
        if (totalCount < requiredCount) {
            return false; // 物品数量不足
        }

        // 移除指定数量的物品
        removeItem(player, targetStack, requiredCount);
        return true;
    }

    /**
     * 获取玩家身上指定物品的数量。
     *
     * @param player      玩家
     * @param targetStack 目标物品堆栈
     * @return 物品数量
     */
    private static int getItemCount(ServerPlayer player, ItemStack targetStack) {
        int count = 0;
        NonNullList<ItemStack> inventory = player.getInventory().items; // 获取主物品栏
        for (ItemStack stack : inventory) {
            if (ItemStack.isSameItemSameComponents(stack, targetStack)) { // 检查物品类型和 NBT 是否匹配
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 移除玩家身上指定数量的指定物品。
     *
     * @param player      玩家
     * @param targetStack 目标物品堆栈
     * @param count       数量
     */
    private static void removeItem(ServerPlayer player, ItemStack targetStack, int count) {
        NonNullList<ItemStack> inventory = player.getInventory().items; // 获取主物品栏
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (ItemStack.isSameItemSameComponents(stack, targetStack)) { // 检查物品类型和 NBT 是否匹配
                int removeAmount = Math.min(stack.getCount(), count);
                stack.shrink(removeAmount); // 减少物品数量
                count -= removeAmount;
                if (count <= 0) {
                    break; // 已经移除足够数量的物品
                }
            }
        }
    }
}
