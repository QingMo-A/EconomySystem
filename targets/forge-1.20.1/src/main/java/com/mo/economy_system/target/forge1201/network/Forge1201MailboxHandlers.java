package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.client.ClientMailboxSendState;
import com.mo.economy_system.common.client.ClientMailboxState;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimResult;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimService;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.mail.MailboxCapacityPolicy;
import com.mo.economy_system.common.mail.MailboxLedger;
import com.mo.economy_system.common.mail.MailboxQueryService;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MailboxClaimAllMessage;
import com.mo.economy_system.common.network.MailboxClaimAttachmentMessage;
import com.mo.economy_system.common.network.MailboxDataRequestMessage;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxDeleteMessage;
import com.mo.economy_system.common.network.MailboxMarkReadMessage;
import com.mo.economy_system.common.network.MailboxNotificationMessage;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendResultMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mo.economy_system.target.forge1201.client.Forge1201MailboxNotifications;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

/** Forge server/client handlers for the full mailbox protocol. */
final class Forge1201MailboxHandlers {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<UUID, Long> LAST_PLAYER_MAIL_SEND = new java.util.concurrent.ConcurrentHashMap<>();

  private Forge1201MailboxHandlers() {}

  static void request(MailboxDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> refresh(player, message.requestId()));
    context.setPacketHandled(true);
  }

  static void response(MailboxDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMailboxState.update(message));
    context.setPacketHandled(true);
  }

  static void markRead(MailboxMarkReadMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
      mailbox.ledger().markRead(player.getUUID(), message.mailId(), mailbox::markDirty);
      refresh(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void delete(MailboxDeleteMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
      MailboxLedger.MutationResult result =
          mailbox.ledger().delete(player.getUUID(), message.mailId(), mailbox::markDirty);
      if (result == MailboxLedger.MutationResult.HAS_ATTACHMENTS) {
        player.sendSystemMessage(Component.translatable("message.mailbox.delete.has_attachments"));
      }
      refresh(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void claimAttachment(
      MailboxClaimAttachmentMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      DeliveryBoxClaimResult result = claimOne(player, message.mailId(), message.entryId(), message.requestId());
      player.sendSystemMessage(Component.translatable(feedbackKey(result)));
      refresh(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void claimAll(MailboxClaimAllMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
      MailRecord mail = mailbox.ledger().findPersonal(player.getUUID(), message.mailId());
      if (mail == null) {
        player.sendSystemMessage(Component.translatable("message.delivery.claim.not_found"));
        refresh(player, message.requestId());
        return;
      }
      DeliveryBoxClaimResult last = DeliveryBoxClaimResult.SUCCESS;
      int claimed = 0;
      for (UUID entryId : List.copyOf(mail.unclaimedAttachmentIds())) {
        last = claimOne(player, mail.mailId(), entryId, message.requestId());
        if (last != DeliveryBoxClaimResult.SUCCESS) break;
        claimed++;
      }
      if (claimed > 0) {
        player.sendSystemMessage(Component.translatable("message.mailbox.claim_all.success", claimed));
      }
      if (last != DeliveryBoxClaimResult.SUCCESS) {
        player.sendSystemMessage(Component.translatable(feedbackKey(last)));
      } else {
        CurrencyClaimResult currency = claimCurrencyReward(player, mail.mailId());
        if (currency.status() == CurrencyClaimStatus.CLAIMED) {
          player.sendSystemMessage(Component.translatable(
              "message.mailbox.currency_reward.claimed", currency.amount()));
        } else if (currency.status() == CurrencyClaimStatus.BALANCE_LIMIT) {
          player.sendSystemMessage(Component.translatable("message.mailbox.currency_reward.balance_limit"));
        } else if (currency.status() == CurrencyClaimStatus.FAILED) {
          player.sendSystemMessage(Component.translatable("message.mailbox.currency_reward.failed"));
        }
      }
      refresh(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void sendPlayer(MailboxSendPlayerMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer sender = context.getSender();
    if (sender != null) context.enqueueWork(() -> {
      MailboxSendStatus status;
      if (!acquireSendCooldown(sender.getUUID())) {
        status = MailboxSendStatus.RATE_LIMITED;
      } else try {
        status = sendPlayerMail(sender, message);
      } catch (RuntimeException failure) {
        LOGGER.error("Player mail send failed sender={} recipient={}", sender.getUUID(), message.recipientName(), failure);
        status = MailboxSendStatus.FAILED;
      }
      Forge1201NetworkChannel.sendToPlayer(sender, new MailboxSendResultMessage(message.requestId(), status));
      sender.sendSystemMessage(Component.translatable(sendFeedbackKey(status)));
    });
    context.setPacketHandled(true);
  }

  static void sendResult(MailboxSendResultMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMailboxSendState.update(message));
    context.setPacketHandled(true);
  }

  static void notification(MailboxNotificationMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
      ClientMailboxState.invalidate();
      Forge1201MailboxNotifications.show(message);
    });
    context.setPacketHandled(true);
  }

  static void notifyNewMail(ServerPlayer recipient, MailType type, String senderName, String subject) {
    if (recipient == null) return;
    try {
      Forge1201NetworkChannel.sendToPlayer(
          recipient, new MailboxNotificationMessage(type, senderName, subject));
    } catch (RuntimeException failure) {
      LOGGER.warn("Mailbox toast delivery failed recipient={} type={}",
          recipient.getUUID(), type, failure);
    }
  }

  private static DeliveryBoxClaimResult claimOne(
      ServerPlayer player, UUID mailId, UUID entryId, long requestId) {
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
    MailRecord mail = mailbox.ledger().findPersonal(player.getUUID(), mailId);
    if (mail == null || !mail.unclaimedAttachmentIds().contains(entryId)) return DeliveryBoxClaimResult.NOT_FOUND;
    Forge1201DeliveryBoxSavedData delivery = Forge1201DeliveryBoxSavedData.get(player.serverLevel());
    DeliveryBoxEntrySnapshot claimedEntry = delivery.ledger().list(player.getUUID()).stream()
        .filter(entry -> entry.entryId().equals(entryId)).findFirst().orElse(null);
    if (claimedEntry == null) return DeliveryBoxClaimResult.NOT_FOUND;
    DeliveryBoxClaimResult result = DeliveryBoxClaimService.claim(
        new DeliveryBoxClaimMessage(entryId, requestId),
        new DeliveryBoxClaimService.Context(
            player.getUUID(), delivery.ledger(),
            entry -> Forge1201Platform.nativeItemStacks()
                .restoreSnapshot(entry.item(), player.serverLevel().registryAccess()).orElseThrow(),
            new Forge1201TransactionalInventoryAdapter(player), delivery::markDirty,
            (owner, entry, stage, outcome, error) -> LOGGER.error(
                "Mailbox claim failed owner={} entry={} stage={} result={}",
                owner, entry, stage, outcome, error)));
    if (result == DeliveryBoxClaimResult.SUCCESS) {
      try {
        mailbox.ledger().markAttachmentClaimed(player.getUUID(), mailId,
            new com.mo.economy_system.common.mail.MailAttachmentSnapshot(
                claimedEntry.entryId(), claimedEntry.item(), true), mailbox::markDirty);
      } catch (RuntimeException metadataFailure) {
        LOGGER.error("Mailbox attachment claim-history update failed owner={} mail={} entry={}",
            player.getUUID(), mailId, entryId, metadataFailure);
      }
    }
    return result;
  }

  /** Claims a deferred reward only after all item attachments in the same request succeeded. */
  private static CurrencyClaimResult claimCurrencyReward(ServerPlayer player, UUID mailId) {
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
    MailRecord mail = mailbox.ledger().findPersonal(player.getUUID(), mailId);
    if (mail == null || !mail.hasCurrencyReward() || mail.currencyRewardClaimed()) {
      return CurrencyClaimResult.SKIPPED;
    }
    int amount = mail.currencyRewardAmount();
    EconomySavedData economy = EconomySavedData.getInstance(player.serverLevel());
    if (economy.previewCreditExact(player.getUUID(), amount)
        != BalanceMutationResult.SUCCESS) {
      return CurrencyClaimResult.BALANCE_LIMIT;
    }
    var credited = economy.creditExact(player.getUUID(), amount, "委托奖励", "领取委托货币奖励");
    if (credited != BalanceMutationResult.SUCCESS) {
      return credited == BalanceMutationResult.BALANCE_LIMIT
          ? CurrencyClaimResult.BALANCE_LIMIT : CurrencyClaimResult.FAILED;
    }
    try {
      MailboxLedger.MutationResult marked = mailbox.ledger().markCurrencyRewardClaimed(
          player.getUUID(), mailId, mailbox::markDirty);
      if (marked == MailboxLedger.MutationResult.UPDATED) {
        return CurrencyClaimResult.claimed(amount);
      }
    } catch (RuntimeException failure) {
      LOGGER.error("Deferred mailbox currency reward marker failed player={} mail={} amount={}",
          player.getUUID(), mailId, amount, failure);
    }
    var rollback = economy.debitExact(player.getUUID(), amount, "委托奖励回滚", "委托货币奖励领取失败回滚");
    if (rollback != BalanceMutationResult.SUCCESS) {
      LOGGER.error("Deferred mailbox currency reward rollback failed player={} mail={} amount={} result={}",
          player.getUUID(), mailId, amount, rollback);
    }
    return CurrencyClaimResult.FAILED;
  }

  private enum CurrencyClaimStatus { SKIPPED, CLAIMED, BALANCE_LIMIT, FAILED }

  private record CurrencyClaimResult(CurrencyClaimStatus status, int amount) {
    private static final CurrencyClaimResult SKIPPED = new CurrencyClaimResult(CurrencyClaimStatus.SKIPPED, 0);
    private static final CurrencyClaimResult BALANCE_LIMIT =
        new CurrencyClaimResult(CurrencyClaimStatus.BALANCE_LIMIT, 0);
    private static final CurrencyClaimResult FAILED = new CurrencyClaimResult(CurrencyClaimStatus.FAILED, 0);

    private static CurrencyClaimResult claimed(int amount) {
      return new CurrencyClaimResult(CurrencyClaimStatus.CLAIMED, amount);
    }
  }

  private static MailboxSendStatus sendPlayerMail(ServerPlayer sender, MailboxSendPlayerMessage message) {
    ServerPlayer onlineRecipient = sender.server.getPlayerList().getPlayerByName(message.recipientName());
    UUID recipientId;
    if (onlineRecipient != null) {
      recipientId = onlineRecipient.getUUID();
    } else {
      var profile = sender.server.getProfileCache().get(message.recipientName());
      if (profile.isEmpty() || profile.get().getId() == null) return MailboxSendStatus.RECIPIENT_NOT_FOUND;
      recipientId = profile.get().getId();
    }
    if (recipientId.equals(sender.getUUID())) return MailboxSendStatus.CANNOT_SEND_TO_SELF;

    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(sender.serverLevel());
    Forge1201DeliveryBoxSavedData delivery = Forge1201DeliveryBoxSavedData.get(sender.serverLevel());
    if (!MailboxCapacityPolicy.canAddPersonal(
        MailType.PLAYER, mailbox.ledger().listPersonal(recipientId).size())) {
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    }
    if (!MailboxCapacityPolicy.canAddPlayerDeliveries(
        delivery.ledger().list(recipientId).size(), message.inventorySlots().size())) {
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    }

    int moneyAmount = message.moneyAmount();
    EconomySavedData economy = moneyAmount > 0
        ? EconomySavedData.getInstance(sender.serverLevel()) : null;
    if (economy != null) {
      MailboxSendStatus financialStatus = mapTransferFailure(
          economy.previewTransferExact(sender.getUUID(), recipientId, moneyAmount));
      if (financialStatus != MailboxSendStatus.SUCCESS) return financialStatus;
    }

    Map<Integer, ItemStack> originals = new LinkedHashMap<>();
    List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>();
    for (int slot : message.inventorySlots()) {
      ItemStack stack = sender.getInventory().items.get(slot);
      if (stack.isEmpty()) return MailboxSendStatus.INVALID_ATTACHMENT;
      ItemStack copy = stack.copy();
      originals.put(slot, copy);
      var snapshot = Forge1201Platform.nativeItemStacks()
          .captureSnapshot(copy, sender.serverLevel().registryAccess());
      if (!snapshot.isSuccess()) return MailboxSendStatus.INVALID_ATTACHMENT;
      entries.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot.orElseThrow(), "mail.player"));
    }

    boolean deliveryAdded = false;
    boolean moneyTransferred = false;
    try {
      if (economy != null) {
        BalanceTransferResult transfer = economy.transferExact(
            sender.getUUID(), recipientId, moneyAmount,
            "邮件转账", "发送邮件金额", "接收邮件金额");
        MailboxSendStatus financialStatus = mapTransferFailure(transfer);
        if (financialStatus != MailboxSendStatus.SUCCESS) return financialStatus;
        moneyTransferred = true;
      }
      for (int slot : originals.keySet()) sender.getInventory().setItem(slot, ItemStack.EMPTY);
      if (!entries.isEmpty()) {
        delivery.ledger().addAll(recipientId, entries, delivery::markDirty);
        deliveryAdded = true;
      }
      List<UUID> attachmentIds = entries.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
      long now = System.currentTimeMillis();
      mailbox.ledger().addPersonal(recipientId,
          new MailRecord(UUID.randomUUID(), MailType.PLAYER, sender.getUUID(),
              sender.getGameProfile().getName(), message.subject(), message.body(), "mail.player",
              now, 0, attachmentIds, moneyAmount, false, false), mailbox::markDirty);
    } catch (IllegalStateException full) {
      rollbackSend(sender, recipientId, originals, entries, deliveryAdded, delivery,
          economy, moneyAmount, moneyTransferred);
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    } catch (RuntimeException failure) {
      rollbackSend(sender, recipientId, originals, entries, deliveryAdded, delivery,
          economy, moneyAmount, moneyTransferred);
      throw failure;
    }

    // Mail + attachments are committed at this point. UI synchronization and online-player
    // notification are best-effort side effects and must not roll back persisted item ownership.
    try {
      sender.containerMenu.broadcastChanges();
    } catch (RuntimeException refreshFailure) {
      LOGGER.warn("Player-mail sender inventory refresh failed sender={} recipient={}",
          sender.getUUID(), recipientId, refreshFailure);
    }
    if (onlineRecipient != null) {
      try {
        notifyNewMail(onlineRecipient, MailType.PLAYER, sender.getGameProfile().getName(), message.subject());
      } catch (RuntimeException notificationFailure) {
        LOGGER.warn("Player-mail toast notification failed sender={} recipient={}",
            sender.getUUID(), recipientId, notificationFailure);
      }
    }
    return MailboxSendStatus.SUCCESS;
  }

  private static void rollbackSend(
      ServerPlayer sender, UUID recipientId, Map<Integer, ItemStack> originals,
      List<DeliveryBoxEntrySnapshot> entries, boolean deliveryAdded, Forge1201DeliveryBoxSavedData delivery,
      EconomySavedData economy, int moneyAmount, boolean moneyTransferred) {
    boolean safeToRestoreSender = true;
    if (deliveryAdded) {
      Set<UUID> ids = new LinkedHashSet<>();
      for (DeliveryBoxEntrySnapshot entry : entries) ids.add(entry.entryId());
      try {
        safeToRestoreSender = delivery.ledger().removeUnclaimedBatch(recipientId, ids, delivery::markDirty);
      } catch (RuntimeException rollbackFailure) {
        safeToRestoreSender = false;
        LOGGER.error("Failed to roll back player-mail delivery entries recipient={}; sender items will not be restored to avoid duplication",
            recipientId, rollbackFailure);
      }
    }
    if (moneyTransferred && economy != null) {
      BalanceTransferResult refund = economy.transferExact(
          recipientId, sender.getUUID(), moneyAmount,
          "邮件回滚", "邮件发送失败退款", "邮件发送失败回退");
      if (refund != BalanceTransferResult.SUCCESS) {
        safeToRestoreSender = false;
        LOGGER.error(
            "Failed to roll back player-mail money transfer sender={} recipient={} amount={} result={}; sender items will not be restored",
            sender.getUUID(), recipientId, moneyAmount, refund);
      }
    }
    if (safeToRestoreSender) {
      originals.forEach((slot, stack) -> sender.getInventory().setItem(slot, stack.copy()));
      sender.containerMenu.broadcastChanges();
    }
  }

  private static void refresh(ServerPlayer player, long requestId) {
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(player.serverLevel());
    Forge1201DeliveryBoxSavedData delivery = Forge1201DeliveryBoxSavedData.get(player.serverLevel());
    try {
      Forge1201NetworkChannel.sendToPlayer(player, MailboxDataResponseMessage.data(requestId,
          MailboxQueryService.query(player.getUUID(), mailbox.ledger(), delivery.ledger(),
              System.currentTimeMillis(), mailbox::markDirty)));
    } catch (RuntimeException failure) {
      LOGGER.error("Mailbox refresh failed player={} request={}", player.getUUID(), requestId, failure);
      Forge1201NetworkChannel.sendToPlayer(player, MailboxDataResponseMessage.error(requestId));
    }
  }

  private static boolean acquireSendCooldown(UUID senderId) {
    long now = System.currentTimeMillis();
    Long previous = LAST_PLAYER_MAIL_SEND.put(senderId, now);
    return previous == null || now - previous >= EconomyNetworkLimits.PLAYER_MAIL_COOLDOWN_MILLIS;
  }

  private static MailboxSendStatus mapTransferFailure(BalanceTransferResult result) {
    if (result == null) return MailboxSendStatus.FAILED;
    return switch (result) {
      case SUCCESS -> MailboxSendStatus.SUCCESS;
      case INSUFFICIENT_FUNDS -> MailboxSendStatus.INSUFFICIENT_FUNDS;
      case RECIPIENT_BALANCE_LIMIT -> MailboxSendStatus.RECIPIENT_BALANCE_LIMIT;
      case INVALID_AMOUNT, SAME_ACCOUNT, PERSIST_FAILED, TARGET_NOT_AVAILABLE -> MailboxSendStatus.FAILED;
    };
  }

  private static String feedbackKey(DeliveryBoxClaimResult result) {
    return switch (result) {
      case SUCCESS -> "message.delivery.claim.success";
      case NOT_FOUND -> "message.delivery.claim.not_found";
      case INVENTORY_FULL -> "message.delivery.claim.inventory_full";
      case ITEM_RESTORE_FAILED, INVALID_ENTRY -> "message.delivery.claim.invalid_item";
      case PERSIST_FAILED -> "message.delivery.claim.persist_failed";
      case ROLLBACK_FAILED, STATE_UNKNOWN -> "message.delivery.claim.state_unknown";
      case INVENTORY_FAILED -> "message.delivery.claim.inventory_failed";
    };
  }

  private static String sendFeedbackKey(MailboxSendStatus status) {
    return "message.mailbox.send." + status.id();
  }
}
