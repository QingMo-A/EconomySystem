package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211PurchaseSalesOrderHandler {
  private NeoForge1211PurchaseSalesOrderHandler() {}

  public static void handle(PurchaseSalesOrderMessage m, IPayloadContext c) {
    c.enqueueWork(() -> execute(m, c));
  }

  private static void execute(PurchaseSalesOrderMessage m, IPayloadContext c) {
    if (!(c.player() instanceof ServerPlayer buyer)) return;
    PurchaseSalesOrderOutcome o;
    EconomySavedData accounts;
    try {
      accounts = EconomySavedData.getInstance(buyer.serverLevel());
      MarketSavedData market = MarketSavedData.getInstance(buyer.serverLevel());
      NeoForge1211TransactionalInventoryAdapter inventory =
          new NeoForge1211TransactionalInventoryAdapter(buyer);
      o =
          PurchaseSalesOrderService.execute(
              m,
              new PurchaseSalesOrderService.Context(
                  buyer.getUUID(),
                  inventory,
                  inventory,
                  new Accounts(accounts, buyer),
                  new Repository(market),
                  reporter()));
    } catch (RuntimeException e) {
      EconomySystem.LOGGER.error(
          "Sales purchase infrastructure failed buyer={} tradeId={}",
          buyer.getUUID(),
          m.tradeId(),
          e);
      sendGenericFailure(buyer, m.tradeId());
      return;
    }
    IsolatedPostActions.runAll(
        MarketActionPostPlan.build(
            o.mutationState(),
            o.result() == PurchaseSalesOrderResult.SUCCESS,
            o.result() == PurchaseSalesOrderResult.SUCCESS,
            () -> MarketInvalidationBroadcaster.broadcast(buyer),
            () -> feedback(buyer, o),
            () -> notifySeller(buyer, accounts, o.purchasedOrder().orElseThrow())),
        (stage, exception) ->
            EconomySystem.LOGGER.error(
                "Sales purchase post-commit failure stage={} tradeId={}",
                stage,
                m.tradeId(),
                exception));
  }

  private static void feedback(ServerPlayer buyer, PurchaseSalesOrderOutcome o) {
    if (o.result() != PurchaseSalesOrderResult.SUCCESS) {
      buyer.sendSystemMessage(Component.translatable(PurchaseSalesOrderFeedback.key(o.result())));
      return;
    }
    MarketOrder order = o.purchasedOrder().orElseThrow();
    buyer.sendSystemMessage(
        Component.translatable(
            PurchaseSalesOrderFeedback.key(o.result()),
            displayName(buyer, order),
            order.quantity(),
            order.totalPrice()));
  }

  private static Object displayName(ServerPlayer p, MarketOrder o) {
    try {
      return NeoForge1211Platform.nativeItemStacks()
          .restoreSnapshot(o.item(), p.registryAccess())
          .orElseThrow()
          .getHoverName();
    } catch (RuntimeException e) {
      EconomySystem.LOGGER.error("Purchase display item restore failed tradeId={}", o.tradeId(), e);
      return o.item().itemId();
    }
  }

  private static void notifySeller(
      ServerPlayer buyer, EconomySavedData accounts, MarketOrder order) {
    Component notice =
        Component.translatable(
            "message.market.purchase.seller_notice",
            displayName(buyer, order),
            order.quantity(),
            buyer.getName(),
            order.totalPrice());
    ServerPlayer seller = buyer.server.getPlayerList().getPlayer(order.sellerId());
    if (seller != null) seller.sendSystemMessage(notice);
    else accounts.storeOfflineMessage(order.sellerId(), notice.getString());
  }

  private static void sendGenericFailure(ServerPlayer buyer, UUID tradeId) {
    try {
      buyer.sendSystemMessage(Component.translatable("message.market.purchase.failed"));
    } catch (RuntimeException exception) {
      EconomySystem.LOGGER.error(
          "Sales purchase infrastructure feedback failed tradeId={}", tradeId, exception);
    }
  }

  private record Repository(MarketSavedData d) implements PurchaseSalesOrderService.Repository {
    public MarketOrder find(UUID id) {
      return d.getOrder(id);
    }

    public SalesOrderRemovalResult removeSalesTransactional(UUID id) {
      return d.removeSalesTransactional(id);
    }
  }

  private record Accounts(EconomySavedData d, ServerPlayer buyer)
      implements PurchaseSalesOrderService.Accounts {
    public BalanceTransferResult preview(UUID seller, int amount) {
      return d.previewTransferExact(buyer.getUUID(), seller, amount);
    }

    public BalanceTransferResult transfer(UUID seller, int amount) {
      return d.transferExact(buyer.getUUID(), seller, amount, "市场交易", "购买销售订单", "销售订单收入");
    }
  }

  private static PurchaseSalesOrderService.FailureReporter reporter() {
    return (t, b, s, g, r, i, o, e) ->
        EconomySystem.LOGGER.error(
            "Sales purchase failure tradeId={} buyer={} seller={} stage={} result={}"
                + " inventoryRollback={} orderRestore={}",
            t,
            b,
            s,
            g,
            r,
            i,
            o,
            e);
  }
}
