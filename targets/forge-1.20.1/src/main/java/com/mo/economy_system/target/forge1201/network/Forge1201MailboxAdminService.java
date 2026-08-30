package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class Forge1201MailboxAdminService {
  private Forge1201MailboxAdminService() {}

  public static UUID resolvePlayer(MinecraftServer server, String name) {
    var online = server.getPlayerList().getPlayerByName(name);
    if (online != null) return online.getUUID();
    var profile = server.getProfileCache().get(name);
    return profile.isPresent() ? profile.get().getId() : null;
  }

  public static void sendNotice(ServerLevel level, UUID recipient, String subject, String body) {
    sendNotice(level, recipient, subject, body, 0);
  }

  /** Sends a protected system mail and immediately credits its optional money amount. */
  public static void sendNotice(
      ServerLevel level, UUID recipient, String subject, String body, int moneyAmount) {
    requireMoneyAmount(moneyAmount);
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(level);
    if (mailbox.ledger().listPersonal(recipient).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
      throw new IllegalStateException("recipient mailbox is full");
    }
    EconomySavedData economy = moneyAmount > 0 ? EconomySavedData.getInstance(level) : null;
    if (economy != null && economy.previewCreditExact(recipient, moneyAmount)
        != BalanceMutationResult.SUCCESS) {
      throw new IllegalStateException("recipient balance limit reached");
    }
    boolean moneyCredited = false;
    try {
      if (economy != null) {
        BalanceMutationResult credited = economy.creditExact(
            recipient, moneyAmount, "邮件", "系统邮件发放");
        if (credited != BalanceMutationResult.SUCCESS) {
          throw new IllegalStateException("system mail money credit failed: " + credited);
        }
        moneyCredited = true;
      }
      long now = System.currentTimeMillis();
      mailbox.ledger().addPersonal(recipient,
          new MailRecord(UUID.randomUUID(), MailType.SYSTEM, null, "", subject, body,
              "mail.system", now, 0, List.of(), moneyAmount, false, true), mailbox::markDirty);
    } catch (RuntimeException failure) {
      if (moneyCredited && economy != null) rollbackCredit(economy, recipient, moneyAmount, failure);
      throw failure;
    }
    Forge1201MailboxHandlers.notifyNewMail(
        level.getServer().getPlayerList().getPlayer(recipient), MailType.SYSTEM, "", subject);
  }

  public static void announce(ServerLevel level, String subject, String body, long expiresAtEpochMillis) {
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(level);
    long now = System.currentTimeMillis();
    mailbox.ledger().addAnnouncement(
        new MailRecord(UUID.randomUUID(), MailType.ANNOUNCEMENT, null, "", subject, body,
            "mail.announcement", now, Math.max(0, expiresAtEpochMillis), List.of(), false, true),
        mailbox::markDirty);
    for (var player : level.getServer().getPlayerList().getPlayers()) {
      Forge1201MailboxHandlers.notifyNewMail(player, MailType.ANNOUNCEMENT, "", subject);
    }
  }

  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body, ItemStack stack) {
    sendCompensation(level, recipient, subject, body, List.of(stack), 0);
  }

  /** Sends an item compensation mail with an immediately credited money amount. */
  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body,
      ItemStack stack, int moneyAmount) {
    sendCompensation(level, recipient, subject, body, List.of(stack), moneyAmount);
  }

  /** Sends one protected compensation mail with multiple independently claimable item attachments. */
  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body, List<ItemStack> stacks) {
    sendCompensation(level, recipient, subject, body, stacks, 0);
  }

  /** Sends one protected compensation mail with item attachments and optional money. */
  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body,
      List<ItemStack> stacks, int moneyAmount) {
    requireMoneyAmount(moneyAmount);
    if (stacks == null || stacks.isEmpty()) throw new IllegalArgumentException("compensation items are empty");
    if (stacks.size() > EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) {
      throw new IllegalArgumentException("too many compensation attachments");
    }

    // Capture every native stack before mutating either ledger.
    List<DeliveryBoxEntrySnapshot> attachments = new ArrayList<>(stacks.size());
    for (ItemStack stack : stacks) {
      if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("compensation item is empty");
      var snapshot = Forge1201Platform.nativeItemStacks().captureSnapshot(stack.copy(), level.registryAccess())
          .orElseThrow();
      attachments.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot, "mail.compensation"));
    }

    Forge1201DeliveryBoxSavedData delivery = Forge1201DeliveryBoxSavedData.get(level);
    Forge1201MailboxSavedData mailbox = Forge1201MailboxSavedData.get(level);
    if (mailbox.ledger().listPersonal(recipient).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
      throw new IllegalStateException("recipient mailbox is full");
    }
    if (delivery.ledger().list(recipient).size() + attachments.size()
        > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
      throw new IllegalStateException("recipient attachment storage is full");
    }
    EconomySavedData economy = moneyAmount > 0 ? EconomySavedData.getInstance(level) : null;
    if (economy != null && economy.previewCreditExact(recipient, moneyAmount)
        != BalanceMutationResult.SUCCESS) {
      throw new IllegalStateException("recipient balance limit reached");
    }

    boolean deliveryAdded = false;
    boolean moneyCredited = false;
    List<UUID> attachmentIds = attachments.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
    try {
      delivery.ledger().addAll(recipient, attachments, delivery::markDirty);
      deliveryAdded = true;
      if (economy != null) {
        BalanceMutationResult credited = economy.creditExact(
            recipient, moneyAmount, "邮件", "补偿邮件发放");
        if (credited != BalanceMutationResult.SUCCESS) {
          throw new IllegalStateException("compensation mail money credit failed: " + credited);
        }
        moneyCredited = true;
      }
      long now = System.currentTimeMillis();
      mailbox.ledger().addPersonal(recipient,
          new MailRecord(UUID.randomUUID(), MailType.COMPENSATION, null, "", subject, body,
              "mail.compensation", now, 0, attachmentIds, moneyAmount, false, true), mailbox::markDirty);
    } catch (RuntimeException failure) {
      if (deliveryAdded) rollbackDelivery(delivery, recipient, attachmentIds, failure);
      if (moneyCredited && economy != null) rollbackCredit(economy, recipient, moneyAmount, failure);
      throw failure;
    }
    Forge1201MailboxHandlers.notifyNewMail(
        level.getServer().getPlayerList().getPlayer(recipient), MailType.COMPENSATION, "", subject);
  }

  private static void requireMoneyAmount(int moneyAmount) {
    if (moneyAmount < 0) throw new IllegalArgumentException("money amount must be non-negative");
  }

  private static void rollbackDelivery(
      Forge1201DeliveryBoxSavedData delivery, UUID recipient, List<UUID> attachmentIds,
      RuntimeException original) {
    try {
      if (!delivery.ledger().removeUnclaimedBatch(
          recipient, Set.copyOf(attachmentIds), delivery::markDirty)) {
        original.addSuppressed(new IllegalStateException("compensation delivery rollback was incomplete"));
      }
    } catch (RuntimeException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }

  private static void rollbackCredit(
      EconomySavedData economy, UUID recipient, int moneyAmount, RuntimeException original) {
    try {
      BalanceMutationResult result = economy.debitExact(
          recipient, moneyAmount, "邮件回滚", "邮件发送失败退款");
      if (result != BalanceMutationResult.SUCCESS) {
        original.addSuppressed(new IllegalStateException("money rollback failed: " + result));
      }
    } catch (RuntimeException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }
}
