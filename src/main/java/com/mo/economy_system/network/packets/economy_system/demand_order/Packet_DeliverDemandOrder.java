package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/** Legacy NeoForge payload retained until protocol 14 is formally migrated. */
public class Packet_DeliverDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {
    public static final Type<Packet_DeliverDemandOrder> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "economy_system/demand_order/packet_deliver_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_DeliverDemandOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> encode(packet, buf), Packet_DeliverDemandOrder::decode);
    private final UUID itemId;
    public Packet_DeliverDemandOrder(UUID itemId) { this.itemId = itemId; }
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }
    public static void encode(Packet_DeliverDemandOrder msg, FriendlyByteBuf buf) { buf.writeUUID(msg.itemId); }
    public static Packet_DeliverDemandOrder decode(FriendlyByteBuf buf) { return new Packet_DeliverDemandOrder(buf.readUUID()); }

    public static void handle(Packet_DeliverDemandOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer supplier)) return;
            MarketSavedData market = MarketSavedData.getInstance(supplier.serverLevel());
            EconomySavedData accounts = EconomySavedData.getInstance(supplier.serverLevel());
            MarketOrder before = market.getOrder(msg.itemId);
            DemandOrderDeliveryResult result = DemandOrderDeliveryService.execute(msg.itemId,
                    new DemandOrderDeliveryService.Context(supplier.getUUID(), new InventoryAdapter(supplier),
                            new AccountAdapter(accounts, supplier), new RepositoryAdapter(market), reporter(supplier)));
            if (result == DemandOrderDeliveryResult.SUCCESS && before != null) {
                ItemStack display = EconomyServices.platform().itemStacks().restoreSnapshot(before.item(), supplier.registryAccess()).orElseThrow();
                supplier.sendSystemMessage(Component.translatable(Util_MessageKeys.DELIVERY_SUCCESS_KEY, display.getHoverName().getString(), before.quantity()));
                notifyRequester(supplier, accounts, before, display);
            } else {
                supplier.sendSystemMessage(Component.translatable(result == DemandOrderDeliveryResult.INSUFFICIENT_ITEMS
                        ? Util_MessageKeys.DELIVERY_NOT_ENOUGH_ITEMS_KEY : result == DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT
                        ? Util_MessageKeys.DELIVERY_BALANCE_LIMIT_KEY : Util_MessageKeys.MARKET_ITEM_DOES_NOT_EXIST_MESSAGE_KEY));
            }
            MarketInvalidationBroadcaster.broadcast(supplier);
        });
    }

    private static void notifyRequester(ServerPlayer supplier, EconomySavedData accounts, MarketOrder order, ItemStack display) {
        Component message = Component.translatable(Util_MessageKeys.ORDER_DELIVERED_BY_PLAYER_KEY,
                display.getHoverName().getString(), order.quantity(), supplier.getName().getString());
        ServerPlayer requester = supplier.serverLevel().getServer().getPlayerList().getPlayer(order.sellerId());
        if (requester != null) requester.sendSystemMessage(message); else accounts.storeOfflineMessage(order.sellerId(), message.getString());
    }

    private static DemandOrderDeliveryService.FailureReporter reporter(ServerPlayer player) {
        return (tradeId, stage, result, cause, compensation) -> {
            EconomySystem.LOGGER.error("Demand delivery failed supplier={} order={} stage={} result={} paymentReverted={} inventoryRestored={}",
                    player.getUUID(), tradeId, stage, result, compensation.paymentReverted(), compensation.inventoryRestored(), cause);
            if (compensation.paymentError() != null) EconomySystem.LOGGER.error("Demand delivery payment compensation threw order={}", tradeId, compensation.paymentError());
            if (compensation.inventoryError() != null) EconomySystem.LOGGER.error("Demand delivery inventory compensation threw order={}", tradeId, compensation.inventoryError());
        };
    }

    private record RepositoryAdapter(MarketSavedData data) implements DemandOrderDeliveryService.Repository {
        public MarketOrder find(UUID id) { return data.getOrder(id); }
        public DemandDeliveryTransitionResult markDelivered(UUID id) { return data.markDemandDelivered(id); }
    }
    private record AccountAdapter(EconomySavedData data, ServerPlayer player) implements DemandOrderDeliveryService.Account {
        public boolean canCreditExact(int amount) { return data.canCreditExact(player.getUUID(), amount); }
        public BalanceMutationResult creditExact(int amount) { return data.creditExact(player.getUUID(), amount, "市场", "交付求购单"); }
        public BalanceMutationResult debitExact(int amount) { return data.debitExact(player.getUUID(), amount, "市场", "交付求购单回滚"); }
    }
    private record InventoryAdapter(ServerPlayer player) implements DemandOrderDeliveryService.Inventory {
        private Inventory inventory() { return player.getInventory(); }
        public Object restoreTemplate(MarketOrder order) {
            return EconomyServices.platform().itemStacks().restoreSnapshot(order.item(), player.registryAccess()).value().orElse(null);
        }
        public long countMatching(Object value) { ItemStack template=(ItemStack)value;long count=0;
            for(ItemStack stack:inventory().items)if(!stack.isEmpty()&&EconomyServices.platform().itemStacks().sameItemAndData(stack,template))count+=stack.getCount();return count; }
        public DemandOrderDeliveryService.RemovalResult removeMatching(Object value,int quantity) {
            ItemStack template=(ItemStack)value;List<ItemStack> before=inventory().items.stream().map(ItemStack::copy).toList();
            try { int remaining=quantity;for(ItemStack stack:inventory().items)if(remaining>0&&!stack.isEmpty()&&EconomyServices.platform().itemStacks().sameItemAndData(stack,template)){
                    int removed=Math.min(remaining,stack.getCount());stack.shrink(removed);remaining-=removed;}
                if(remaining!=0)return DemandOrderDeliveryService.RemovalResult.failure(restore(before));
                inventory().setChanged();return DemandOrderDeliveryService.RemovalResult.success(()->restore(before));
            } catch(RuntimeException exception){boolean restored=restore(before);EconomySystem.LOGGER.error("Demand delivery item removal failed supplier={} restored={}",player.getUUID(),restored,exception);return DemandOrderDeliveryService.RemovalResult.failure(restored);}
        }
        private boolean restore(List<ItemStack> before){boolean restored=true;for(int i=0;i<before.size();i++)try{inventory().setItem(i,before.get(i).copy());}
            catch(RuntimeException exception){restored=false;EconomySystem.LOGGER.error("Demand delivery inventory restore failed supplier={} slot={}",player.getUUID(),i,exception);}inventory().setChanged();return restored;}
    }
}
