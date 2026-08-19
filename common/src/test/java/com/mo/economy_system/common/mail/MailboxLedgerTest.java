package com.mo.economy_system.common.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MailboxLedgerTest {
  @Test
  void personalMailCannotBeDeletedUntilEveryAttachmentIsClaimed() {
    UUID owner = UUID.randomUUID();
    UUID attachment = UUID.randomUUID();
    UUID mailId = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    ledger.addPersonal(owner, mail(mailId, MailType.PLAYER, 10, 0, List.of(attachment), false), () -> {});

    assertEquals(MailboxLedger.MutationResult.HAS_ATTACHMENTS,
        ledger.delete(owner, mailId, () -> {}));
    assertEquals(1, ledger.listPersonal(owner).size());

    assertEquals(MailboxLedger.MutationResult.UPDATED,
        ledger.removeAttachment(owner, mailId, attachment, () -> {}));
    assertEquals(MailboxLedger.MutationResult.DELETED,
        ledger.delete(owner, mailId, () -> {}));
    assertTrue(ledger.listPersonal(owner).isEmpty());
  }

  @Test
  void announcementReadAndDismissStateIsPerPlayer() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID mailId = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    ledger.addAnnouncement(mail(mailId, MailType.ANNOUNCEMENT, 10, 0, List.of(), true), () -> {});

    assertFalse(ledger.listAnnouncements(first, 20).get(0).read());
    assertFalse(ledger.listAnnouncements(second, 20).get(0).read());

    assertEquals(MailboxLedger.MutationResult.UPDATED,
        ledger.markRead(first, mailId, () -> {}));
    assertTrue(ledger.listAnnouncements(first, 20).get(0).read());
    assertFalse(ledger.listAnnouncements(second, 20).get(0).read());

    assertEquals(MailboxLedger.MutationResult.DELETED,
        ledger.delete(first, mailId, () -> {}));
    assertTrue(ledger.listAnnouncements(first, 20).isEmpty());
    assertEquals(1, ledger.listAnnouncements(second, 20).size());
  }

  @Test
  void expiredMailWithAttachmentIsNeverPurgedSilently() {
    UUID owner = UUID.randomUUID();
    UUID attachment = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.COMPENSATION, 10, 20, List.of(attachment), true), () -> {});

    assertEquals(0, ledger.purgeExpired(30, () -> {}));
    assertEquals(1, ledger.listPersonal(owner).size());
    assertEquals(List.of(attachment), ledger.listPersonal(owner).get(0).attachmentIds());
  }

  @Test
  void legacyDeliveryEntriesAreAdoptedOnceAsMailboxMetadata() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 3);
    MailboxLedger ledger = new MailboxLedger();

    assertEquals(1, ledger.adoptLegacy(owner, List.of(entry), 100, () -> {}));
    assertEquals(0, ledger.adoptLegacy(owner, List.of(entry), 101, () -> {}));

    MailRecord adopted = ledger.listPersonal(owner).get(0);
    assertEquals(MailType.MARKET, adopted.type());
    assertEquals(List.of(entry.entryId()), adopted.attachmentIds());
    assertTrue(adopted.protectedMail());
  }

  private static MailRecord mail(
      UUID id, MailType type, long created, long expires, List<UUID> attachments, boolean protectedMail) {
    return new MailRecord(id, type, null, "", "subject", "body", "test.mail",
        created, expires, attachments, false, protectedMail);
  }
}
