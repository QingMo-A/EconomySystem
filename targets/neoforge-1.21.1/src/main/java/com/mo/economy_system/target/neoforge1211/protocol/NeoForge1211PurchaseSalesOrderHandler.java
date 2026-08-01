package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

public final class NeoForge1211PurchaseSalesOrderHandler {
    private NeoForge1211PurchaseSalesOrderHandler() {}

    public static void handle(PurchaseSalesOrderMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer buyer)) return;
            try {
                EconomySavedData accounts = EconomySavedData.getInstance(buyer.serverLevel());
                MarketSavedData market = MarketSavedData.getInstance(buyer.serverLevel());
                PurchaseSalesOrderOutcome outcome = PurchaseSalesOrderService.execute(message,
                        new PurchaseSalesOrderService.Context(buyer.getUUID(), new InventoryAdapter(buyer),
                                new AccountsAdapter(accounts, buyer), new RepositoryAdapter(market), reporter()));
                sendFeedback(buyer, outcome);
                if (outcome.result() == PurchaseSalesOrderResult.SUCCESS) notifySeller(buyer, accounts, outcome.purchasedOrder().orElseThrow());
                if (outcome.marketChanged()) MarketInvalidationBroadcaster.broadcast(buyer);
            } catch (RuntimeException exception) {
                EconomySystem.LOGGER.error("Unhandled sales purchase request buyer={} tradeId={}", buyer.getUUID(), message.tradeId(), exception);
                buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(PurchaseSalesOrderResult.PAYMENT_FAILED)));
            }
        });
    }

    private static void sendFeedback(ServerPlayer buyer, PurchaseSalesOrderOutcome outcome) {
        if (outcome.result() == PurchaseSalesOrderResult.SUCCESS) {
            MarketOrder order = outcome.purchasedOrder().orElseThrow();
            ItemStack stack = EconomyServices.platform().itemStacks().restoreSnapshot(order.item(), buyer.registryAccess()).orElseThrow();
            buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(outcome.result()),
                    stack.getHoverName(), order.quantity(), order.totalPrice()));
        } else buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(outcome.result())));
    }

    private static void notifySeller(ServerPlayer buyer, EconomySavedData accounts, MarketOrder order) {
        try {
            ItemStack stack = EconomyServices.platform().itemStacks().restoreSnapshot(order.item(), buyer.registryAccess()).orElseThrow();
            Component notice = Component.translatable("message.market.purchase.seller_notice", stack.getHoverName(),
                    order.quantity(), buyer.getName(), order.totalPrice());
            ServerPlayer seller = buyer.server.getPlayerList().getPlayer(order.sellerId());
            if (seller != null) seller.sendSystemMessage(notice);
            else accounts.storeOfflineMessage(order.sellerId(), notice.getString());
        } catch (RuntimeException exception) {
            EconomySystem.LOGGER.error("Completed sale but failed seller notification tradeId={} seller={}", order.tradeId(), order.sellerId(), exception);
        }
    }

    private record RepositoryAdapter(MarketSavedData data) implements PurchaseSalesOrderService.Repository {
        public MarketOrder find(UUID tradeId) { return data.getOrder(tradeId); }
        public SalesOrderRemovalResult removeSalesForPurchase(UUID tradeId) { return data.removeSalesForPurchase(tradeId); }
    }
    private record AccountsAdapter(EconomySavedData data, ServerPlayer buyer) implements PurchaseSalesOrderService.Accounts {
        public BalanceTransferResult preview(UUID sellerId, int amount) { return data.previewTransferExact(buyer.getUUID(), sellerId, amount); }
        public BalanceTransferResult transfer(UUID sellerId, int amount) { return data.transferExact(buyer.getUUID(), sellerId, amount,
                "市场交易", "购买销售订单", "销售订单收入"); }
    }
    private record InventoryAdapter(ServerPlayer player) implements PurchaseSalesOrderService.Inventory {
        public Object restore(MarketOrder order) { return EconomyServices.platform().itemStacks().restoreSnapshot(order.item(), player.registryAccess()).orElseThrow(); }
        public boolean canAccept(Object value, int quantity) { return capacity((ItemStack)value) >= quantity; }
        private int capacity(ItemStack template) { long capacity=0; Inventory inv=player.getInventory(); for(ItemStack stack:inv.items){
            if(stack.isEmpty()) capacity+=template.getMaxStackSize();
            else if(EconomyServices.platform().itemStacks().sameItemAndData(stack,template)) capacity+=Math.max(0,stack.getMaxStackSize()-stack.getCount());
            if(capacity>=Integer.MAX_VALUE)return Integer.MAX_VALUE;}return (int)capacity; }
        public PurchaseSalesOrderService.InsertionResult insert(Object value,int quantity){ItemStack template=((ItemStack)value).copy();template.setCount(1);Inventory inv=player.getInventory();List<ItemStack> before=inv.items.stream().map(ItemStack::copy).toList();try{
            int remaining=quantity;for(ItemStack stack:inv.items)if(remaining>0&&!stack.isEmpty()&&EconomyServices.platform().itemStacks().sameItemAndData(stack,template)){int add=Math.min(remaining,stack.getMaxStackSize()-stack.getCount());if(add>0){stack.grow(add);remaining-=add;}}
            for(int i=0;i<inv.items.size()&&remaining>0;i++)if(inv.items.get(i).isEmpty()){ItemStack inserted=template.copy();int add=Math.min(remaining,inserted.getMaxStackSize());inserted.setCount(add);inv.setItem(i,inserted);remaining-=add;}
            if(remaining!=0)return PurchaseSalesOrderService.InsertionResult.failure(restore(before));inv.setChanged();return PurchaseSalesOrderService.InsertionResult.success(()->restore(before));
        }catch(RuntimeException exception){boolean restored=restore(before);EconomySystem.LOGGER.error("Inventory insertion failed buyer={} restored={}",player.getUUID(),restored,exception);return PurchaseSalesOrderService.InsertionResult.failure(restored);}}
        private boolean restore(List<ItemStack> before){boolean ok=true;for(int i=0;i<before.size();i++)try{player.getInventory().setItem(i,before.get(i).copy());}catch(RuntimeException e){ok=false;EconomySystem.LOGGER.error("Inventory rollback slot failed buyer={} slot={}",player.getUUID(),i,e);}player.getInventory().setChanged();return ok;}
    }
    private static PurchaseSalesOrderService.FailureReporter reporter(){return (tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception)->EconomySystem.LOGGER.error(
            "Sales purchase transaction failure tradeId={} buyer={} seller={} stage={} result={} inventoryRollback={} orderRestore={}",tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception);}
}
