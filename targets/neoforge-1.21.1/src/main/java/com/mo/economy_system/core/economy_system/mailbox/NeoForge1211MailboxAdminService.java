package com.mo.economy_system.core.economy_system.mailbox;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211MailboxHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** System/admin mailbox producer API shared by commands and future gameplay systems. */
public final class NeoForge1211MailboxAdminService {
  private NeoForge1211MailboxAdminService() {}

  public static UUID resolvePlayer(MinecraftServer server, String name) {
    var online = server.getPlayerList().getPlayerByName(name);
    if (online != null) return online.getUUID();
    var profile = server.getProfileCache().get(name);
    return profile.isPresent() ? profile.get().getId() : null;
  }

  public static void sendNotice(ServerLevel level, UUID recipient, String subject, String body) {
    MailboxSavedData mailbox = MailboxSavedData.getInstance(level);
    long now = System.currentTimeMillis();
    mailbox.ledger().addPersonal(recipient,
        new MailRecord(UUID.randomUUID(), MailType.SYSTEM, null, "", subject, body,
            "mail.system", now, 0, List.of(), false, true), mailbox::markDirty);
    NeoForge1211MailboxHandlers.notifyNewMail(
        level.getServer().getPlayerList().getPlayer(recipient), MailType.SYSTEM, "", subject);
  }

  public static void announce(ServerLevel level, String subject, String body, long expiresAtEpochMillis) {
    MailboxSavedData mailbox = MailboxSavedData.getInstance(level);
    long now = System.currentTimeMillis();
    mailbox.ledger().addAnnouncement(
        new MailRecord(UUID.randomUUID(), MailType.ANNOUNCEMENT, null, "", subject, body,
            "mail.announcement", now, Math.max(0, expiresAtEpochMillis), List.of(), false, true),
        mailbox::markDirty);
    for (var player : level.getServer().getPlayerList().getPlayers()) {
      NeoForge1211MailboxHandlers.notifyNewMail(player, MailType.ANNOUNCEMENT, "", subject);
    }
  }

  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body, ItemStack stack) {
    sendCompensation(level, recipient, subject, body, List.of(stack));
  }

  /** Sends one protected compensation mail with multiple independently claimable item attachments. */
  public static void sendCompensation(
      ServerLevel level, UUID recipient, String subject, String body, List<ItemStack> stacks) {
    if (stacks == null || stacks.isEmpty()) throw new IllegalArgumentException("compensation items are empty");
    if (stacks.size() > EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) {
      throw new IllegalArgumentException("too many compensation attachments");
    }

    // Capture every native stack before mutating either ledger.
    List<DeliveryBoxEntrySnapshot> attachments = new ArrayList<>(stacks.size());
    for (ItemStack stack : stacks) {
      if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("compensation item is empty");
      var snapshot = NeoForge1211Platform.nativeItemStacks().captureSnapshot(stack.copy(), level.registryAccess())
          .orElseThrow();
      attachments.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot, "mail.compensation"));
    }

    DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(level);
    MailboxSavedData mailbox = MailboxSavedData.getInstance(level);
    if (mailbox.ledger().listPersonal(recipient).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
      throw new IllegalStateException("recipient mailbox is full");
    }
    if (delivery.ledger().list(recipient).size() + attachments.size()
        > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
      throw new IllegalStateException("recipient attachment storage is full");
    }

    delivery.ledger().addAll(recipient, attachments, delivery::markDirty);
    List<UUID> attachmentIds = attachments.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
    try {
      long now = System.currentTimeMillis();
      mailbox.ledger().addPersonal(recipient,
          new MailRecord(UUID.randomUUID(), MailType.COMPENSATION, null, "", subject, body,
              "mail.compensation", now, 0, attachmentIds, false, true), mailbox::markDirty);
    } catch (RuntimeException failure) {
      try {
        delivery.ledger().removeUnclaimedBatch(recipient, Set.copyOf(attachmentIds), delivery::markDirty);
      } catch (RuntimeException rollbackFailure) {
        failure.addSuppressed(rollbackFailure);
      }
      throw failure;
    }
    NeoForge1211MailboxHandlers.notifyNewMail(
        level.getServer().getPlayerList().getPlayer(recipient), MailType.COMPENSATION, "", subject);
  }
}
