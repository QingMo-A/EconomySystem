package com.mo.economy_system.common.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
  void deferredCurrencyRewardBlocksDeletionAndClaimIsIdempotent() {
    UUID owner = UUID.randomUUID();
    UUID mailId = UUID.randomUUID();
    UUID rewardRecordId = UUID.randomUUID();
    MailRecord reward = new MailRecord(
        mailId, MailType.SYSTEM, null, "", "Reward", "Body", "mail.commission",
        10, 0, List.of(), rewardRecordId, 200, false, false, true);
    MailboxLedger ledger = new MailboxLedger();
    AtomicInteger dirtyCalls = new AtomicInteger();
    ledger.addPersonal(owner, reward, dirtyCalls::incrementAndGet);

    assertEquals(MailboxLedger.MutationResult.HAS_ATTACHMENTS,
        ledger.delete(owner, mailId, dirtyCalls::incrementAndGet));
    assertEquals(MailboxLedger.MutationResult.UPDATED,
        ledger.markCurrencyRewardClaimed(owner, mailId, dirtyCalls::incrementAndGet));
    assertEquals(MailboxLedger.MutationResult.NO_CHANGE,
        ledger.markCurrencyRewardClaimed(owner, mailId, dirtyCalls::incrementAndGet));
    assertEquals(2, dirtyCalls.get(), "only add and first claim should dirty the ledger");
    assertTrue(ledger.listPersonal(owner).get(0).currencyRewardClaimed());
    assertEquals(MailboxLedger.MutationResult.DELETED,
        ledger.delete(owner, mailId, dirtyCalls::incrementAndGet));
  }

  @Test
  void queryProjectionExposesDeferredCurrencyReward() {
    UUID owner = UUID.randomUUID();
    UUID rewardRecordId = UUID.randomUUID();
    MailRecord reward = new MailRecord(
        UUID.randomUUID(), MailType.SYSTEM, null, "", "Reward", "Body", "mail.commission",
        10, 0, List.of(), rewardRecordId, 200, false, false, true);
    MailboxLedger ledger = new MailboxLedger();
    ledger.addPersonal(owner, reward, () -> {});

    MailSnapshot projected = MailboxQueryService.query(
        owner, ledger, new com.mo.economy_system.common.delivery.DeliveryBoxLedger(), 20, () -> {})
        .get(0);

    assertEquals(rewardRecordId, projected.rewardRecordId());
    assertEquals(200, projected.currencyRewardAmount());
    assertTrue(projected.hasUnclaimedCurrencyReward());
    assertTrue(projected.hasUnclaimedAttachments());
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
  void globalAnnouncementsRejectMoneyBecauseTheyHaveNoIndividualRecipient() {
    MailboxLedger ledger = new MailboxLedger();
    MailRecord announcement = new MailRecord(
        UUID.randomUUID(), MailType.ANNOUNCEMENT, null, "", "subject", "body", "test.mail",
        10, 0, List.of(), 25, false, true);

    assertThrows(IllegalArgumentException.class,
        () -> ledger.addAnnouncement(announcement, () -> {}));
  }

  @Test
  void globalAnnouncementsRejectDeferredCurrencyRewards() {
    MailboxLedger ledger = new MailboxLedger();
    MailRecord announcement = new MailRecord(
        UUID.randomUUID(), MailType.ANNOUNCEMENT, null, "", "subject", "body", "test.mail",
        10, 0, List.of(), UUID.randomUUID(), 25, false, false, true);

    assertThrows(IllegalArgumentException.class,
        () -> ledger.addAnnouncement(announcement, () -> {}));
  }

  @Test
  void monetaryMailMetadataSurvivesReadAndSnapshotProjection() {
    UUID owner = UUID.randomUUID();
    MailRecord original = new MailRecord(
        UUID.randomUUID(), MailType.SYSTEM, null, "", "subject", "body", "test.mail",
        10, 0, List.of(), 250, false, true);
    MailboxLedger ledger = new MailboxLedger();
    ledger.addPersonal(owner, original, () -> {});

    assertEquals(250, ledger.listPersonal(owner).get(0).moneyAmount());
    ledger.markRead(owner, original.mailId(), () -> {});
    assertEquals(250, ledger.listPersonal(owner).get(0).moneyAmount());
    assertEquals(250, ledger.snapshot().personal().get(owner).get(0).moneyAmount());
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

  @Test
  void playerMailStopsAtSoftCapWhileCriticalMailUsesReservedTail() {
    UUID owner = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    for (int i = 0; i < MailboxCapacityPolicy.PLAYER_MAIL_LIMIT; i++) {
      ledger.addPersonal(owner,
          mail(UUID.randomUUID(), MailType.PLAYER, i + 1L, 0, List.of(), false), () -> {});
    }

    assertThrows(IllegalStateException.class, () -> ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.PLAYER, 1_000, 0, List.of(), false), () -> {}));

    ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.MARKET, 1_001, 0, List.of(), true), () -> {});
    ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.COMPENSATION, 1_002, 0, List.of(), true), () -> {});
    ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.SYSTEM, 1_003, 0, List.of(), true), () -> {});
    assertEquals(MailboxCapacityPolicy.PLAYER_MAIL_LIMIT + 3, ledger.listPersonal(owner).size());
  }

  @Test
  void criticalMailStillStopsAtPhysicalHardLimit() {
    UUID owner = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    for (int i = 0; i < com.mo.economy_system.common.network.EconomyNetworkLimits.MAX_MAILS_PER_PLAYER; i++) {
      ledger.addPersonal(owner,
          mail(UUID.randomUUID(), MailType.MARKET, i + 1L, 0, List.of(), true), () -> {});
    }
    assertThrows(IllegalStateException.class, () -> ledger.addPersonal(owner,
        mail(UUID.randomUUID(), MailType.MARKET, 10_000, 0, List.of(), true), () -> {}));
  }

  @Test
  void exactRollbackRefusesToDeleteMailThatChanged() {
    UUID owner = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    MailRecord original = mail(UUID.randomUUID(), MailType.MARKET, 10, 0, List.of(), true);
    ledger.addPersonal(owner, original, () -> {});
    ledger.markRead(owner, original.mailId(), () -> {});

    assertFalse(ledger.removePersonalIfUnchanged(owner, original, () -> {}));
    assertEquals(1, ledger.listPersonal(owner).size());
  }

  @Test
  void exactRollbackDeletesOnlyTheExpectedUnchangedMail() {
    UUID owner = UUID.randomUUID();
    MailboxLedger ledger = new MailboxLedger();
    MailRecord first = mail(UUID.randomUUID(), MailType.MARKET, 10, 0, List.of(), true);
    MailRecord second = mail(UUID.randomUUID(), MailType.SYSTEM, 11, 0, List.of(), true);
    ledger.addPersonal(owner, first, () -> {});
    ledger.addPersonal(owner, second, () -> {});

    assertTrue(ledger.removePersonalIfUnchanged(owner, first, () -> {}));
    assertEquals(List.of(second), ledger.listPersonal(owner));
  }

  private static MailRecord mail(
      UUID id, MailType type, long created, long expires, List<UUID> attachments, boolean protectedMail) {
    return new MailRecord(id, type, null, "", "subject", "body", "test.mail",
        created, expires, attachments, false, protectedMail);
  }
}
