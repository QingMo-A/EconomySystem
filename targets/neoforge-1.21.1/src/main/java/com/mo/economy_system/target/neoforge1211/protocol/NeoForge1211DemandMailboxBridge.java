package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.mail.MailboxCapacityPolicy;
import com.mo.economy_system.common.market.DemandMailboxResult;
import com.mo.economy_system.common.market.DemandOrderDeliveryService;
import com.mo.economy_system.common.market.DemandSettlementMailContent;
import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.RemoveSalesOrderService;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.mailbox.MailboxSavedData;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Staged mailbox attachment bridge used by demand-order settlement. */
final class NeoForge1211DemandMailboxBridge
    implements DemandOrderDeliveryService.Mailbox, RemoveSalesOrderService.Mailbox {
  private final ServerPlayer supplier;

  NeoForge1211DemandMailboxBridge(ServerPlayer supplier) {
    this.supplier = supplier;
  }

  @Override
  public DemandMailboxResult preflight(UUID requesterId, Object template, int quantity) {
    if (!(template instanceof ItemStack stack) || stack.isEmpty() || quantity <= 0) {
      return DemandMailboxResult.FAILED;
    }
    try {
      int attachmentCount = attachmentCount(stack, quantity);
      if (attachmentCount > EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) return DemandMailboxResult.FULL;
      MailboxSavedData mailbox = MailboxSavedData.getInstance(supplier.serverLevel());
      DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(supplier.serverLevel());
      if (!MailboxCapacityPolicy.canAddPersonal(
          MailType.MARKET, mailbox.ledger().listPersonal(requesterId).size())) {
        return DemandMailboxResult.FULL;
      }
      if (!MailboxCapacityPolicy.canAddCriticalDeliveries(
          delivery.ledger().list(requesterId).size(), attachmentCount)) {
        return DemandMailboxResult.FULL;
      }
      return DemandMailboxResult.SUCCESS;
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error("Demand mailbox preflight failed requester={}", requesterId, error);
      return DemandMailboxResult.FAILED;
    }
  }

  @Override
  public DemandOrderDeliveryService.MailboxStage stage(
      UUID requesterId,
      MarketOrder fulfilledSlice,
      Object template,
      int quantity,
      int amount,
      int remainingQuantity) {
    DemandMailboxResult ready = preflight(requesterId, template, quantity);
    if (ready != DemandMailboxResult.SUCCESS) {
      return DemandOrderDeliveryService.MailboxStage.failure(ready);
    }
    ItemStack nativeTemplate = (ItemStack) template;

    List<DeliveryBoxEntrySnapshot> attachments;
    try {
      DemandSettlementMailContent content = DemandSettlementMailContent.create(
          fulfilledSlice, supplier.getGameProfile().getName(), amount, remainingQuantity);
      attachments = snapshots(nativeTemplate, quantity, content.source());
      List<UUID> attachmentIds = attachments.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
      MailRecord mail = new MailRecord(
          UUID.randomUUID(),
          MailType.MARKET,
          supplier.getUUID(),
          supplier.getGameProfile().getName(),
          content.subject(),
          content.body(),
          content.source(),
          System.currentTimeMillis(),
          0,
          attachmentIds,
          false,
          true);

      DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(supplier.serverLevel());
      MailboxSavedData mailbox = MailboxSavedData.getInstance(supplier.serverLevel());
      delivery.ledger().addAll(requesterId, attachments, delivery::markDirty);
      try {
        mailbox.ledger().addPersonal(requesterId, mail, mailbox::markDirty);
      } catch (RuntimeException metadataFailure) {
        DemandMailboxResult rollback = rollbackAttachments(
            delivery, requesterId, new LinkedHashSet<>(attachmentIds));
        if (rollback != DemandMailboxResult.SUCCESS) {
          EconomySystem.LOGGER.error(
              "Demand mailbox metadata failed and attachment rollback is uncertain tradeId={} requester={}",
              fulfilledSlice.tradeId(), requesterId, metadataFailure);
          return DemandOrderDeliveryService.MailboxStage.failure(DemandMailboxResult.UNKNOWN);
        }
        EconomySystem.LOGGER.error(
            "Demand mailbox metadata failed after attachments were cleanly rolled back tradeId={} requester={}",
            fulfilledSlice.tradeId(), requesterId, metadataFailure);
        return DemandOrderDeliveryService.MailboxStage.failure(DemandMailboxResult.FAILED);
      }

      return DemandOrderDeliveryService.MailboxStage.success(
          new StagedDelivery(requesterId, mail, attachments, mailbox, delivery));
    } catch (IllegalStateException full) {
      return DemandOrderDeliveryService.MailboxStage.failure(DemandMailboxResult.FULL);
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error(
          "Demand mailbox stage failed tradeId={} requester={} quantity={} amount={} remaining={}",
          fulfilledSlice.tradeId(), requesterId, quantity, amount, remainingQuantity, error);
      return DemandOrderDeliveryService.MailboxStage.failure(DemandMailboxResult.FAILED);
    }
  }

  @Override
  public DemandMailboxResult deliver(
      UUID ownerId, MarketOrder order, Object template, int quantity) {
    DemandMailboxResult ready = preflight(ownerId, template, quantity);
    if (ready != DemandMailboxResult.SUCCESS) return ready;
    if (!(template instanceof ItemStack nativeTemplate)) return DemandMailboxResult.FAILED;

    String source = "market.admin_removed:" + order.tradeId();
    try {
      List<DeliveryBoxEntrySnapshot> attachments = snapshots(nativeTemplate, quantity, source);
      List<UUID> attachmentIds = attachments.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
      MailRecord mail = new MailRecord(
          UUID.randomUUID(),
          MailType.MARKET,
          supplier.getUUID(),
          supplier.getGameProfile().getName(),
          "管理员下架商品退回",
          "管理员 " + supplier.getGameProfile().getName()
              + " 已下架你的市场出售订单，剩余物品已退回至附件。",
          source,
          System.currentTimeMillis(),
          0,
          attachmentIds,
          false,
          true);

      DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(supplier.serverLevel());
      MailboxSavedData mailbox = MailboxSavedData.getInstance(supplier.serverLevel());
      delivery.ledger().addAll(ownerId, attachments, delivery::markDirty);
      try {
        mailbox.ledger().addPersonal(ownerId, mail, mailbox::markDirty);
      } catch (RuntimeException metadataFailure) {
        DemandMailboxResult rollback = rollbackAttachments(
            delivery, ownerId, new LinkedHashSet<>(attachmentIds));
        if (rollback != DemandMailboxResult.SUCCESS) {
          EconomySystem.LOGGER.error(
              "Admin sales removal mail metadata failed and attachment rollback is uncertain tradeId={} owner={}",
              order.tradeId(), ownerId, metadataFailure);
          return DemandMailboxResult.UNKNOWN;
        }
        EconomySystem.LOGGER.error(
            "Admin sales removal mail metadata failed after clean attachment rollback tradeId={} owner={}",
            order.tradeId(), ownerId, metadataFailure);
        return DemandMailboxResult.FAILED;
      }

      try {
        NeoForge1211MailboxHandlers.notifyNewMail(
            supplier.server.getPlayerList().getPlayer(ownerId), MailType.MARKET,
            supplier.getGameProfile().getName(), mail.subject());
      } catch (RuntimeException notificationFailure) {
        EconomySystem.LOGGER.warn(
            "Admin sales removal mail notification failed owner={}", ownerId, notificationFailure);
      }
      return DemandMailboxResult.SUCCESS;
    } catch (IllegalStateException full) {
      return DemandMailboxResult.FULL;
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error(
          "Admin sales removal mail delivery failed tradeId={} owner={} quantity={}",
          order.tradeId(), ownerId, quantity, error);
      return DemandMailboxResult.FAILED;
    }
  }

  private final class StagedDelivery implements DemandOrderDeliveryService.StagedMailboxDelivery {
    private final UUID requesterId;
    private final MailRecord mail;
    private final List<DeliveryBoxEntrySnapshot> attachments;
    private final MailboxSavedData mailbox;
    private final DeliveryBoxSavedData delivery;
    private boolean closed;

    private StagedDelivery(
        UUID requesterId,
        MailRecord mail,
        List<DeliveryBoxEntrySnapshot> attachments,
        MailboxSavedData mailbox,
        DeliveryBoxSavedData delivery) {
      this.requesterId = requesterId;
      this.mail = mail;
      this.attachments = List.copyOf(attachments);
      this.mailbox = mailbox;
      this.delivery = delivery;
    }

    @Override
    public void commit() {
      if (closed) return;
      closed = true;
      try {
        NeoForge1211MailboxHandlers.notifyNewMail(
            supplier.server.getPlayerList().getPlayer(requesterId), MailType.MARKET,
            supplier.getGameProfile().getName(), mail.subject());
      } catch (RuntimeException notificationFailure) {
        EconomySystem.LOGGER.warn("Demand mailbox notification failed requester={}", requesterId,
            notificationFailure);
      }
    }

    @Override
    public DemandMailboxResult rollback() {
      if (closed) return DemandMailboxResult.UNKNOWN;
      boolean mailRemoved;
      try {
        mailRemoved = mailbox.ledger().removePersonalIfUnchanged(requesterId, mail, mailbox::markDirty);
      } catch (RuntimeException error) {
        EconomySystem.LOGGER.error("Demand staged-mail metadata rollback failed requester={} mail={}",
            requesterId, mail.mailId(), error);
        return DemandMailboxResult.UNKNOWN;
      }
      if (!mailRemoved) return DemandMailboxResult.UNKNOWN;

      Set<UUID> ids = new LinkedHashSet<>();
      for (DeliveryBoxEntrySnapshot entry : attachments) ids.add(entry.entryId());
      DemandMailboxResult attachmentRollback = rollbackAttachments(delivery, requesterId, ids);
      if (attachmentRollback != DemandMailboxResult.SUCCESS) {
        // removeUnclaimedBatch is all-or-nothing. Restore the exact metadata record so we do not
        // knowingly leave still-owned delivery entries orphaned from the mailbox UI.
        try {
          mailbox.ledger().addPersonal(requesterId, mail, mailbox::markDirty);
        } catch (RuntimeException restoreFailure) {
          EconomySystem.LOGGER.error(
              "Demand staged-mail rollback could not restore metadata requester={} mail={}",
              requesterId, mail.mailId(), restoreFailure);
        }
        return DemandMailboxResult.UNKNOWN;
      }
      closed = true;
      return DemandMailboxResult.SUCCESS;
    }
  }

  private static DemandMailboxResult rollbackAttachments(
      DeliveryBoxSavedData delivery, UUID requesterId, Set<UUID> attachmentIds) {
    try {
      return delivery.ledger().removeUnclaimedBatch(
          requesterId, attachmentIds, delivery::markDirty)
          ? DemandMailboxResult.SUCCESS : DemandMailboxResult.UNKNOWN;
    } catch (RuntimeException rollbackFailure) {
      return DemandMailboxResult.UNKNOWN;
    }
  }

  private List<DeliveryBoxEntrySnapshot> snapshots(
      ItemStack template, int quantity, String source) {
    List<DeliveryBoxEntrySnapshot> result = new ArrayList<>();
    int remaining = quantity;
    int max = Math.max(1, template.getMaxStackSize());
    while (remaining > 0) {
      int count = Math.min(remaining, max);
      ItemStack stack = template.copy();
      stack.setCount(count);
      var snapshot = NeoForge1211Platform.nativeItemStacks()
          .captureSnapshot(stack, supplier.registryAccess())
          .orElseThrow();
      result.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot, source));
      remaining -= count;
    }
    return List.copyOf(result);
  }

  private static int attachmentCount(ItemStack template, int quantity) {
    int max = Math.max(1, template.getMaxStackSize());
    return (quantity + max - 1) / max;
  }
}
