package com.mo.economy_system.common.mail;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxLedger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds a client mailbox view by joining metadata with authoritative delivery attachments. */
public final class MailboxQueryService {
  private MailboxQueryService() {}

  public static List<MailSnapshot> query(
      UUID ownerId,
      MailboxLedger mailbox,
      DeliveryBoxLedger delivery,
      long nowEpochMillis,
      MailboxLedger.DirtyMarker dirty) {
    List<DeliveryBoxEntrySnapshot> deliveries = delivery.list(ownerId);
    Set<UUID> existing = deliveries.stream().map(DeliveryBoxEntrySnapshot::entryId)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    mailbox.reconcileAttachments(ownerId, existing, dirty);
    mailbox.adoptLegacy(ownerId, deliveries, nowEpochMillis, dirty);
    mailbox.purgeExpired(nowEpochMillis, dirty);

    Map<UUID, DeliveryBoxEntrySnapshot> byId = new LinkedHashMap<>();
    for (DeliveryBoxEntrySnapshot entry : delivery.list(ownerId)) byId.put(entry.entryId(), entry);

    List<MailSnapshot> result = new ArrayList<>();
    for (MailRecord mail : mailbox.listPersonal(ownerId)) {
      result.add(resolve(mail, false, byId));
    }
    for (MailRecord announcement : mailbox.listAnnouncements(ownerId, nowEpochMillis)) {
      result.add(resolve(announcement, true, Map.of()));
    }
    result.sort(Comparator.comparingLong(MailSnapshot::createdAtEpochMillis).reversed()
        .thenComparing(mail -> mail.mailId().toString()));
    return List.copyOf(result);
  }

  private static MailSnapshot resolve(
      MailRecord mail, boolean announcement, Map<UUID, DeliveryBoxEntrySnapshot> deliveries) {
    List<MailAttachmentSnapshot> attachments = new ArrayList<>();
    Map<UUID, MailAttachmentSnapshot> claimedById = new LinkedHashMap<>();
    for (MailAttachmentSnapshot claimed : mail.claimedAttachments()) claimedById.put(claimed.entryId(), claimed);
    for (UUID id : mail.attachmentIds()) {
      MailAttachmentSnapshot claimed = claimedById.get(id);
      if (claimed != null) {
        attachments.add(claimed);
        continue;
      }
      DeliveryBoxEntrySnapshot entry = deliveries.get(id);
      if (entry != null) attachments.add(new MailAttachmentSnapshot(entry.entryId(), entry.item(), false));
    }
    return new MailSnapshot(mail.mailId(), mail.type(), mail.senderId(), mail.senderName(), mail.subject(),
        mail.body(), mail.source(), mail.createdAtEpochMillis(), mail.expiresAtEpochMillis(), mail.read(),
        announcement, mail.protectedMail(), List.copyOf(attachments), mail.moneyAmount());
  }
}
