package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.market.MarketExpirationFeedback;
import com.mo.economy_system.common.market.MarketExpirationOutcome;
import com.mo.economy_system.common.market.MarketExpirationResult;
import com.mo.economy_system.common.market.MarketExpirationService;
import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.MarketOrderRemovalResult;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/** Forge API adapter for the common market expiration transaction. */
public final class Forge1201MarketExpirationRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201MarketExpirationRuntime() {}

  public static void expire(MinecraftServer server) {
    if (server == null) return;
    ServerLevel level = server.overworld();
    MarketSavedData market = MarketSavedData.getInstance(level);
    EconomySavedData economy = EconomySavedData.getInstance(level);
    Forge1201DeliveryBoxSavedData delivery = Forge1201DeliveryBoxSavedData.get(level);
    List<MarketExpirationOutcome> outcomes;
    try {
      outcomes = MarketExpirationService.expire(
          System.currentTimeMillis(),
          new MarketExpirationService.Context(
              new Repository(market),
              economy::creditExact,
              (ownerId, item, quantity, source) -> enqueue(level, delivery, ownerId, item, quantity, source),
              (tradeId, ownerId, stage, result, failure) -> LOGGER.error(
                  "Market expiration failed trade={} owner={} stage={} result={}",
                  tradeId, ownerId, stage, result, failure)));
    } catch (RuntimeException failure) {
      LOGGER.error("Market expiration scan failed", failure);
      return;
    }

    boolean changed = false;
    for (MarketExpirationOutcome outcome : outcomes) {
      if (!outcome.succeeded()) {
        if (outcome.result() == MarketExpirationResult.STATE_UNKNOWN) {
          LOGGER.error("Market expiration state is unknown for trade={}", outcome.order().tradeId());
        }
        continue;
      }
      changed = true;
      try {
        notifyOwner(server, level, economy, outcome);
      } catch (RuntimeException failure) {
        // The transaction is already committed. Notification failure must not
        // prevent the remaining expired orders from being processed.
        LOGGER.error("Market expiration notification failed trade={}", outcome.order().tradeId(), failure);
      }
    }
    if (changed) {
      try {
        Forge1201MarketInvalidation.broadcast(server, level);
      } catch (RuntimeException failure) {
        // A stale client refresh is recoverable; the persisted market result is
        // authoritative and will be sent by the next normal refresh.
        LOGGER.error("Market expiration invalidation broadcast failed", failure);
      }
    }
  }

  private static boolean enqueue(
      ServerLevel level,
      Forge1201DeliveryBoxSavedData delivery,
      UUID ownerId,
      com.mo.economy_system.platform.item.ItemStackSnapshot item,
      int quantity,
      String source) {
    try {
      Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(level);
      if (mailbox.ledger().listPersonal(ownerId).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
        LOGGER.warn("Expired market delivery deferred because mailbox is full owner={}", ownerId);
        return false;
      }
      ItemStack template = Forge1201Platform.nativeItemStacks()
          .restoreSnapshot(item, level.registryAccess())
          .orElseThrow();
      int remaining = quantity;
      List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>();
      while (remaining > 0) {
        int count = Math.min(remaining, template.getMaxStackSize());
        ItemStack stack = template.copy();
        stack.setCount(count);
        var snapshot = Forge1201Platform.nativeItemStacks()
            .captureSnapshot(stack, level.registryAccess())
            .orElseThrow();
        entries.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot, source));
        remaining -= count;
      }
      delivery.ledger().addAll(ownerId, entries, delivery::markDirty);
      boolean mailAdded = false;
      try {
        long now = System.currentTimeMillis();
        mailbox.ledger().addPersonal(ownerId,
            new MailRecord(UUID.randomUUID(), MailType.MARKET, null, "", "", "", source,
                now, 0, entries.stream().map(DeliveryBoxEntrySnapshot::entryId).toList(), false, true),
            mailbox::markDirty);
        mailAdded = true;
      } catch (RuntimeException metadataFailure) {
        // The attachment delivery is authoritative. If mailbox metadata cannot be created,
        // MailboxQueryService will safely adopt these legacy-style entries on the next refresh.
        LOGGER.warn("Expired market delivery metadata fallback owner={}", ownerId, metadataFailure);
      }
      if (mailAdded) {
        Forge1201MailboxHandlers.notifyNewMail(
            level.getServer().getPlayerList().getPlayer(ownerId), MailType.MARKET, "", "");
      }
      return true;
    } catch (RuntimeException failure) {
      // Snapshot conversion happens before mutation and DeliveryBoxLedger.addAll is atomic.
      LOGGER.error("Unable to enqueue expired market order owner={}", ownerId, failure);
      return false;
    }
  }

  private static void notifyOwner(
      MinecraftServer server,
      ServerLevel level,
      EconomySavedData economy,
      MarketExpirationOutcome outcome) {
    MarketOrder order = outcome.order();
    if (outcome.result() == MarketExpirationResult.RETURNED_TO_DELIVERY) return;
    String itemName = itemName(level, order);
    Component message;
    if (outcome.result() == MarketExpirationResult.REFUNDED) {
      message = Component.translatable(MarketExpirationFeedback.DEMAND_REFUNDED,
          itemName, order.totalPrice());
    } else if (order.type() == MarketOrderType.DEMAND) {
      message = Component.translatable(MarketExpirationFeedback.DEMAND_DELIVERED,
          itemName, order.quantity());
    } else {
      message = Component.translatable(MarketExpirationFeedback.SALES_RETURN,
          itemName, order.quantity());
    }
    ServerPlayer owner = server.getPlayerList().getPlayer(order.sellerId());
    if (owner != null) owner.sendSystemMessage(message);
    else economy.storeOfflineMessage(order.sellerId(), message.getString());
  }

  private static String itemName(ServerLevel level, MarketOrder order) {
    try {
      return Forge1201Platform.nativeItemStacks()
          .restoreSnapshot(order.item(), level.registryAccess())
          .orElseThrow()
          .getHoverName()
          .getString();
    } catch (RuntimeException failure) {
      return order.item().itemId();
    }
  }

  private record Repository(MarketSavedData data) implements MarketExpirationService.Repository {
    public List<MarketOrder> orders() {
      return data.getOrders();
    }

    public MarketOrderRemovalResult removeIfUnchanged(MarketOrder expected) {
      return data.removeOrderIfUnchanged(expected);
    }
  }
}
