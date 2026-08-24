package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
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
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.mailbox.MailboxSavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211MailboxNotifications;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge 1.21.1 handlers for the full mailbox protocol. */
public final class NeoForge1211MailboxHandlers {
  private static final Map<UUID, Long> LAST_PLAYER_MAIL_SEND = new java.util.concurrent.ConcurrentHashMap<>();

  private NeoForge1211MailboxHandlers() {}

  public static void request(MailboxDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer player) refresh(player, message.requestId());
    });
  }

  public static void response(MailboxDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientMailboxState.update(message));
  }

  public static void markRead(MailboxMarkReadMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      MailboxSavedData mailbox = MailboxSavedData.getInstance(player.serverLevel());
      mailbox.ledger().markRead(player.getUUID(), message.mailId(), mailbox::markDirty);
      refresh(player, message.requestId());
    });
  }

  public static void delete(MailboxDeleteMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      MailboxSavedData mailbox = MailboxSavedData.getInstance(player.serverLevel());
      MailboxLedger.MutationResult result = mailbox.ledger()
          .delete(player.getUUID(), message.mailId(), mailbox::markDirty);
      if (result == MailboxLedger.MutationResult.HAS_ATTACHMENTS) {
        player.sendSystemMessage(Component.translatable("message.mailbox.delete.has_attachments"));
      }
      refresh(player, message.requestId());
    });
  }

  public static void claimAttachment(MailboxClaimAttachmentMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      DeliveryBoxClaimResult result = claimOne(player, message.mailId(), message.entryId(), message.requestId());
      player.sendSystemMessage(Component.translatable(feedbackKey(result)));
      refresh(player, message.requestId());
    });
  }

  public static void claimAll(MailboxClaimAllMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      MailboxSavedData mailbox = MailboxSavedData.getInstance(player.serverLevel());
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
      if (claimed > 0) player.sendSystemMessage(Component.translatable("message.mailbox.claim_all.success", claimed));
      if (last != DeliveryBoxClaimResult.SUCCESS) player.sendSystemMessage(Component.translatable(feedbackKey(last)));
      refresh(player, message.requestId());
    });
  }

  public static void sendPlayer(MailboxSendPlayerMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer sender)) return;
      MailboxSendStatus status;
      if (!acquireSendCooldown(sender.getUUID())) {
        status = MailboxSendStatus.RATE_LIMITED;
      } else try {
        status = sendPlayerMail(sender, message);
      } catch (RuntimeException failure) {
        EconomySystem.LOGGER.error("Player mail send failed sender={} recipient={}",
            sender.getUUID(), message.recipientName(), failure);
        status = MailboxSendStatus.FAILED;
      }
      EconomySystem_NetworkManager.sendToClient(
          sender, new MailboxSendResultMessage(message.requestId(), status));
      sender.sendSystemMessage(Component.translatable("message.mailbox.send." + status.id()));
    });
  }

  public static void sendResult(MailboxSendResultMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientMailboxSendState.update(message));
  }

  public static void notification(MailboxNotificationMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      ClientMailboxState.invalidate();
      NeoForge1211MailboxNotifications.show(message);
    });
  }

  public static void notifyNewMail(ServerPlayer recipient, MailType type, String senderName, String subject) {
    if (recipient == null) return;
    try {
      EconomySystem_NetworkManager.sendToClient(
          recipient, new MailboxNotificationMessage(type, senderName, subject));
    } catch (RuntimeException failure) {
      EconomySystem.LOGGER.warn("Mailbox toast delivery failed recipient={} type={}",
          recipient.getUUID(), type, failure);
    }
  }

  private static DeliveryBoxClaimResult claimOne(
      ServerPlayer player, UUID mailId, UUID entryId, long requestId) {
    MailboxSavedData mailbox = MailboxSavedData.getInstance(player.serverLevel());
    MailRecord mail = mailbox.ledger().findPersonal(player.getUUID(), mailId);
    if (mail == null || !mail.unclaimedAttachmentIds().contains(entryId)) return DeliveryBoxClaimResult.NOT_FOUND;
    DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(player.serverLevel());
    DeliveryBoxEntrySnapshot claimedEntry = delivery.ledger().list(player.getUUID()).stream()
        .filter(entry -> entry.entryId().equals(entryId)).findFirst().orElse(null);
    if (claimedEntry == null) return DeliveryBoxClaimResult.NOT_FOUND;
    DeliveryBoxClaimResult result = DeliveryBoxClaimService.claim(
        new DeliveryBoxClaimMessage(entryId, requestId),
        new DeliveryBoxClaimService.Context(
            player.getUUID(), delivery.ledger(),
            entry -> NeoForge1211Platform.nativeItemStacks()
                .restoreSnapshot(entry.item(), player.registryAccess()).orElseThrow(),
            new NeoForge1211TransactionalInventoryAdapter(player), delivery::markDirty,
            (owner, entry, stage, outcome, error) -> EconomySystem.LOGGER.error(
                "Mailbox claim failed owner={} entry={} stage={} result={}",
                owner, entry, stage, outcome, error)));
    if (result == DeliveryBoxClaimResult.SUCCESS) {
      try {
        mailbox.ledger().markAttachmentClaimed(player.getUUID(), mailId,
            new com.mo.economy_system.common.mail.MailAttachmentSnapshot(
                claimedEntry.entryId(), claimedEntry.item(), true), mailbox::markDirty);
      } catch (RuntimeException metadataFailure) {
        EconomySystem.LOGGER.error("Mailbox attachment claim-history update failed owner={} mail={} entry={}",
            player.getUUID(), mailId, entryId, metadataFailure);
      }
    }
    return result;
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

    MailboxSavedData mailbox = MailboxSavedData.getInstance(sender.serverLevel());
    DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(sender.serverLevel());
    if (!MailboxCapacityPolicy.canAddPersonal(
        MailType.PLAYER, mailbox.ledger().listPersonal(recipientId).size())) {
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    }
    if (!MailboxCapacityPolicy.canAddPlayerDeliveries(
        delivery.ledger().list(recipientId).size(), message.inventorySlots().size())) {
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    }

    Map<Integer, ItemStack> originals = new LinkedHashMap<>();
    List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>();
    for (int slot : message.inventorySlots()) {
      ItemStack stack = sender.getInventory().items.get(slot);
      if (stack.isEmpty()) return MailboxSendStatus.INVALID_ATTACHMENT;
      ItemStack copy = stack.copy();
      originals.put(slot, copy);
      var snapshot = NeoForge1211Platform.nativeItemStacks().captureSnapshot(copy, sender.registryAccess());
      if (!snapshot.isSuccess()) return MailboxSendStatus.INVALID_ATTACHMENT;
      entries.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot.orElseThrow(), "mail.player"));
    }

    for (int slot : originals.keySet()) sender.getInventory().setItem(slot, ItemStack.EMPTY);
    boolean deliveryAdded = false;
    try {
      if (!entries.isEmpty()) {
        delivery.ledger().addAll(recipientId, entries, delivery::markDirty);
        deliveryAdded = true;
      }
      List<UUID> attachmentIds = entries.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
      long now = System.currentTimeMillis();
      mailbox.ledger().addPersonal(recipientId,
          new MailRecord(UUID.randomUUID(), MailType.PLAYER, sender.getUUID(),
              sender.getGameProfile().getName(), message.subject(), message.body(), "mail.player",
              now, 0, attachmentIds, false, false), mailbox::markDirty);
    } catch (IllegalStateException full) {
      rollbackSend(sender, recipientId, originals, entries, deliveryAdded, delivery);
      return MailboxSendStatus.RECIPIENT_MAILBOX_FULL;
    } catch (RuntimeException failure) {
      rollbackSend(sender, recipientId, originals, entries, deliveryAdded, delivery);
      throw failure;
    }

    // Mail + attachments are committed at this point. Client refresh/notification failures are
    // non-transactional side effects and must never roll back the authoritative item transfer.
    try {
      sender.containerMenu.broadcastChanges();
    } catch (RuntimeException refreshFailure) {
      EconomySystem.LOGGER.warn("Player-mail sender inventory refresh failed sender={} recipient={}",
          sender.getUUID(), recipientId, refreshFailure);
    }
    if (onlineRecipient != null) {
      try {
        notifyNewMail(onlineRecipient, MailType.PLAYER, sender.getGameProfile().getName(), message.subject());
      } catch (RuntimeException notificationFailure) {
        EconomySystem.LOGGER.warn("Player-mail toast notification failed sender={} recipient={}",
            sender.getUUID(), recipientId, notificationFailure);
      }
    }
    return MailboxSendStatus.SUCCESS;
  }

  private static void rollbackSend(
      ServerPlayer sender, UUID recipientId, Map<Integer, ItemStack> originals,
      List<DeliveryBoxEntrySnapshot> entries, boolean deliveryAdded, DeliveryBoxSavedData delivery) {
    boolean safeToRestoreSender = true;
    if (deliveryAdded) {
      Set<UUID> ids = new LinkedHashSet<>();
      for (DeliveryBoxEntrySnapshot entry : entries) ids.add(entry.entryId());
      try {
        safeToRestoreSender = delivery.ledger().removeUnclaimedBatch(recipientId, ids, delivery::markDirty);
      } catch (RuntimeException rollbackFailure) {
        safeToRestoreSender = false;
        EconomySystem.LOGGER.error(
            "Failed to roll back player-mail delivery entries recipient={}; sender items will not be restored to avoid duplication",
            recipientId, rollbackFailure);
      }
    }
    if (safeToRestoreSender) {
      originals.forEach((slot, stack) -> sender.getInventory().setItem(slot, stack.copy()));
      sender.containerMenu.broadcastChanges();
    }
  }

  private static void refresh(ServerPlayer player, long requestId) {
    MailboxSavedData mailbox = MailboxSavedData.getInstance(player.serverLevel());
    DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(player.serverLevel());
    try {
      EconomySystem_NetworkManager.sendToClient(player, MailboxDataResponseMessage.data(requestId,
          MailboxQueryService.query(player.getUUID(), mailbox.ledger(), delivery.ledger(),
              System.currentTimeMillis(), mailbox::markDirty)));
    } catch (RuntimeException failure) {
      EconomySystem.LOGGER.error("Mailbox refresh failed player={} request={}",
          player.getUUID(), requestId, failure);
      EconomySystem_NetworkManager.sendToClient(player, MailboxDataResponseMessage.error(requestId));
    }
  }

  private static boolean acquireSendCooldown(UUID senderId) {
    long now = System.currentTimeMillis();
    Long previous = LAST_PLAYER_MAIL_SEND.put(senderId, now);
    return previous == null || now - previous >= EconomyNetworkLimits.PLAYER_MAIL_COOLDOWN_MILLIS;
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
}
