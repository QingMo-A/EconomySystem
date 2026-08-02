package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

final class Forge1201PurchaseSalesOrderHandler {
    private static final Logger LOGGER=LogUtils.getLogger();
    static void handle(PurchaseSalesOrderMessage message, Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context network=supplier.get();ServerPlayer buyer=network.getSender();if(buyer!=null)try{
        EconomySavedData accounts=EconomySavedData.getInstance(buyer.serverLevel());MarketSavedData market=MarketSavedData.getInstance(buyer.serverLevel());
        InventoryAdapter inventory=new InventoryAdapter(buyer); PurchaseSalesOrderOutcome outcome=PurchaseSalesOrderService.execute(message,new PurchaseSalesOrderService.Context(buyer.getUUID(),inventory,inventory,new AccountsAdapter(accounts,buyer),new RepositoryAdapter(market),reporter()));
        sendFeedback(buyer,outcome);if(outcome.result()==PurchaseSalesOrderResult.SUCCESS)notifySeller(buyer,accounts,outcome.purchasedOrder().orElseThrow());if(outcome.marketChanged())Forge1201MarketInvalidation.broadcast(buyer);
    }catch(RuntimeException exception){LOGGER.error("Unhandled sales purchase request buyer={} tradeId={}",buyer.getUUID(),message.tradeId(),exception);buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(PurchaseSalesOrderResult.PAYMENT_FAILED)));}network.setPacketHandled(true);}
    private static void sendFeedback(ServerPlayer buyer,PurchaseSalesOrderOutcome outcome){if(outcome.result()==PurchaseSalesOrderResult.SUCCESS){MarketOrder order=outcome.purchasedOrder().orElseThrow();ItemStack stack=EconomyServices.platform().itemStacks().restoreSnapshot(order.item(),buyer.serverLevel().registryAccess()).orElseThrow();buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(outcome.result()),stack.getHoverName(),order.quantity(),order.totalPrice()));}else buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(outcome.result())));}
    private static void notifySeller(ServerPlayer buyer,EconomySavedData accounts,MarketOrder order){try{ItemStack stack=EconomyServices.platform().itemStacks().restoreSnapshot(order.item(),buyer.serverLevel().registryAccess()).orElseThrow();Component notice=Component.translatable("message.market.purchase.seller_notice",stack.getHoverName(),order.quantity(),buyer.getName(),order.totalPrice());ServerPlayer seller=buyer.server.getPlayerList().getPlayer(order.sellerId());if(seller!=null)seller.sendSystemMessage(notice);else accounts.storeOfflineMessage(order.sellerId(),notice.getString());}catch(RuntimeException exception){LOGGER.error("Completed sale but failed seller notification tradeId={} seller={}",order.tradeId(),order.sellerId(),exception);}}
    private record RepositoryAdapter(MarketSavedData data)implements PurchaseSalesOrderService.Repository{public MarketOrder find(UUID id){return data.getOrder(id);}public SalesOrderRemovalResult removeSalesTransactional(UUID id){return data.removeSalesTransactional(id);}}
    private record AccountsAdapter(EconomySavedData data,ServerPlayer buyer)implements PurchaseSalesOrderService.Accounts{public BalanceTransferResult preview(UUID seller,int amount){return data.previewTransferExact(buyer.getUUID(),seller,amount);}public BalanceTransferResult transfer(UUID seller,int amount){return data.transferExact(buyer.getUUID(),seller,amount,"市场交易","购买销售订单","销售订单收入");}}
    private record InventoryAdapter(ServerPlayer player)implements MarketItemMaterializer,TransactionalInventory{
        public UUID ownerId(){return player.getUUID();}
        public Object restore(MarketOrder order){return EconomyServices.platform().itemStacks().restoreSnapshot(order.item(),player.serverLevel().registryAccess()).orElseThrow();}
        public boolean canAccept(Object value,int quantity){return capacity((ItemStack)value)>=quantity;}
        private int capacity(ItemStack template){long capacity=0;for(ItemStack stack:player.getInventory().items){if(stack.isEmpty())capacity+=template.getMaxStackSize();else if(EconomyServices.platform().itemStacks().sameItemAndData(stack,template))capacity+=Math.max(0,stack.getMaxStackSize()-stack.getCount());if(capacity>=Integer.MAX_VALUE)return Integer.MAX_VALUE;}return(int)capacity;}
        public InventoryInsertionResult insert(Object value,int quantity){ItemStack template=((ItemStack)value).copy();template.setCount(1);Inventory inv=player.getInventory();List<ItemStack> before=inv.items.stream().map(ItemStack::copy).toList();try{int remaining=quantity;for(ItemStack stack:inv.items)if(remaining>0&&!stack.isEmpty()&&EconomyServices.platform().itemStacks().sameItemAndData(stack,template)){int add=Math.min(remaining,stack.getMaxStackSize()-stack.getCount());if(add>0){stack.grow(add);remaining-=add;}}for(int i=0;i<inv.items.size()&&remaining>0;i++)if(inv.items.get(i).isEmpty()){ItemStack inserted=template.copy();int add=Math.min(remaining,inserted.getMaxStackSize());inserted.setCount(add);inv.setItem(i,inserted);remaining-=add;}if(remaining!=0)return InventoryInsertionResult.failure(restore(before));inv.setChanged();return InventoryInsertionResult.success(()->restore(before));}catch(RuntimeException exception){boolean restored=restore(before);LOGGER.error("Inventory insertion failed buyer={} restored={}",player.getUUID(),restored,exception);return InventoryInsertionResult.failure(restored);}}
        private boolean restore(List<ItemStack> before){boolean ok=true;for(int i=0;i<before.size();i++)try{player.getInventory().setItem(i,before.get(i).copy());}catch(RuntimeException exception){ok=false;LOGGER.error("Inventory rollback slot failed buyer={} slot={}",player.getUUID(),i,exception);}player.getInventory().setChanged();return ok;}}
    private static PurchaseSalesOrderService.FailureReporter reporter(){return(tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception)->LOGGER.error("Sales purchase transaction failure tradeId={} buyer={} seller={} stage={} result={} inventoryRollback={} orderRestore={}",tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception);}
}
