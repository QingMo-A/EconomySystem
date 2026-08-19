package com.mo.economy_system.common.mail;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Thread-safe mailbox metadata ledger. Item attachment payloads remain in DeliveryBoxLedger. */
public final class MailboxLedger {
  private final Map<UUID, List<MailRecord>> personal = new LinkedHashMap<>();
  private final List<MailRecord> announcements = new ArrayList<>();
  private final Map<UUID, Set<UUID>> announcementReads = new LinkedHashMap<>();
  private final Map<UUID, Set<UUID>> announcementDismissed = new LinkedHashMap<>();

  public synchronized List<MailRecord> listPersonal(UUID ownerId) {
    Objects.requireNonNull(ownerId, "ownerId");
    return List.copyOf(personal.getOrDefault(ownerId, List.of()));
  }

  /** Returns visible global announcements with the player's read state projected onto each record. */
  public synchronized List<MailRecord> listAnnouncements(UUID ownerId, long nowEpochMillis) {
    Objects.requireNonNull(ownerId, "ownerId");
    Set<UUID> reads = announcementReads.getOrDefault(ownerId, Set.of());
    Set<UUID> dismissed = announcementDismissed.getOrDefault(ownerId, Set.of());
    List<MailRecord> result = new ArrayList<>();
    for (MailRecord record : announcements) {
      if (record.expired(nowEpochMillis) || dismissed.contains(record.mailId())) continue;
      result.add(record.withRead(reads.contains(record.mailId())));
    }
    return List.copyOf(result);
  }

  public synchronized void addPersonal(UUID ownerId, MailRecord record, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(record, "record");
    Objects.requireNonNull(dirty, "dirty");
    if (record.type() == MailType.ANNOUNCEMENT) {
      throw new IllegalArgumentException("announcement must use global storage");
    }
    List<MailRecord> values = personal.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    if (values.size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
      throw new IllegalStateException("mailbox is full");
    }
    if (findPersonal(ownerId, record.mailId()) != null) {
      throw new IllegalArgumentException("duplicate mail id");
    }
    values.add(record);
    try {
      dirty.markDirty();
    } catch (RuntimeException failure) {
      values.remove(values.size() - 1);
      if (values.isEmpty()) personal.remove(ownerId);
      throw failure;
    }
  }

  public synchronized void addAnnouncement(MailRecord record, DirtyMarker dirty) {
    Objects.requireNonNull(record, "record");
    Objects.requireNonNull(dirty, "dirty");
    if (record.type() != MailType.ANNOUNCEMENT) {
      throw new IllegalArgumentException("global mail must be an announcement");
    }
    if (!record.attachmentIds().isEmpty()) {
      throw new IllegalArgumentException("global announcements cannot carry attachments");
    }
    if (announcements.size() >= EconomyNetworkLimits.MAX_MAIL_ANNOUNCEMENTS) {
      throw new IllegalStateException("announcement store is full");
    }
    if (findAnnouncement(record.mailId()) != null) throw new IllegalArgumentException("duplicate announcement id");
    announcements.add(record.withRead(false));
    try {
      dirty.markDirty();
    } catch (RuntimeException failure) {
      announcements.remove(announcements.size() - 1);
      throw failure;
    }
  }

  public synchronized MutationResult markRead(UUID ownerId, UUID mailId, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(dirty, "dirty");
    List<MailRecord> values = personal.get(ownerId);
    if (values != null) {
      for (int i = 0; i < values.size(); i++) {
        MailRecord record = values.get(i);
        if (!record.mailId().equals(mailId)) continue;
        if (record.read()) return MutationResult.NO_CHANGE;
        values.set(i, record.withRead(true));
        try {
          dirty.markDirty();
          return MutationResult.UPDATED;
        } catch (RuntimeException failure) {
          values.set(i, record);
          throw failure;
        }
      }
    }
    if (findAnnouncement(mailId) != null) {
      Set<UUID> reads = announcementReads.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>());
      if (!reads.add(mailId)) return MutationResult.NO_CHANGE;
      try {
        dirty.markDirty();
        return MutationResult.UPDATED;
      } catch (RuntimeException failure) {
        reads.remove(mailId);
        if (reads.isEmpty()) announcementReads.remove(ownerId);
        throw failure;
      }
    }
    return MutationResult.NOT_FOUND;
  }

  /** Deletes a personal mail only after all attachments are claimed; announcements are dismissed per player. */
  public synchronized MutationResult delete(UUID ownerId, UUID mailId, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(dirty, "dirty");
    List<MailRecord> values = personal.get(ownerId);
    if (values != null) {
      for (int i = 0; i < values.size(); i++) {
        MailRecord record = values.get(i);
        if (!record.mailId().equals(mailId)) continue;
        if (record.hasUnclaimedAttachments()) return MutationResult.HAS_ATTACHMENTS;
        values.remove(i);
        try {
          dirty.markDirty();
          if (values.isEmpty()) personal.remove(ownerId);
          return MutationResult.DELETED;
        } catch (RuntimeException failure) {
          values.add(i, record);
          throw failure;
        }
      }
    }
    if (findAnnouncement(mailId) != null) {
      Set<UUID> dismissed = announcementDismissed.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>());
      if (!dismissed.add(mailId)) return MutationResult.NO_CHANGE;
      try {
        dirty.markDirty();
        return MutationResult.DELETED;
      } catch (RuntimeException failure) {
        dismissed.remove(mailId);
        if (dismissed.isEmpty()) announcementDismissed.remove(ownerId);
        throw failure;
      }
    }
    return MutationResult.NOT_FOUND;
  }

  public synchronized MutationResult removeAttachment(
      UUID ownerId, UUID mailId, UUID entryId, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(entryId, "entryId");
    Objects.requireNonNull(dirty, "dirty");
    List<MailRecord> values = personal.get(ownerId);
    if (values == null) return MutationResult.NOT_FOUND;
    for (int i = 0; i < values.size(); i++) {
      MailRecord record = values.get(i);
      if (!record.mailId().equals(mailId)) continue;
      if (!record.attachmentIds().contains(entryId)) return MutationResult.NOT_FOUND;
      MailRecord updated = record.withoutAttachment(entryId).withRead(true);
      values.set(i, updated);
      try {
        dirty.markDirty();
        return MutationResult.UPDATED;
      } catch (RuntimeException failure) {
        values.set(i, record);
        throw failure;
      }
    }
    return MutationResult.NOT_FOUND;
  }

  /** Moves a successfully claimed attachment into persistent display history. */
  public synchronized MutationResult markAttachmentClaimed(
      UUID ownerId, UUID mailId, MailAttachmentSnapshot attachment, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(attachment, "attachment");
    Objects.requireNonNull(dirty, "dirty");
    List<MailRecord> values = personal.get(ownerId);
    if (values == null) return MutationResult.NOT_FOUND;
    for (int i = 0; i < values.size(); i++) {
      MailRecord record = values.get(i);
      if (!record.mailId().equals(mailId)) continue;
      if (!record.unclaimedAttachmentIds().contains(attachment.entryId())) return MutationResult.NOT_FOUND;
      MailRecord updated = record.withClaimedAttachment(attachment);
      values.set(i, updated);
      try {
        dirty.markDirty();
        return MutationResult.UPDATED;
      } catch (RuntimeException failure) {
        values.set(i, record);
        throw failure;
      }
    }
    return MutationResult.NOT_FOUND;
  }

  /** Drops metadata references for attachments that are no longer present in DeliveryBoxLedger. */
  public synchronized int reconcileAttachments(UUID ownerId, Set<UUID> existingEntryIds, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(existingEntryIds, "existingEntryIds");
    Objects.requireNonNull(dirty, "dirty");
    List<MailRecord> values = personal.get(ownerId);
    if (values == null) return 0;
    List<MailRecord> before = List.copyOf(values);
    int removed = 0;
    for (int i = 0; i < values.size(); i++) {
      MailRecord mail = values.get(i);
      MailRecord updated = mail;
      for (UUID entryId : mail.unclaimedAttachmentIds()) {
        if (!existingEntryIds.contains(entryId)) {
          updated = updated.withoutAttachment(entryId);
          removed++;
        }
      }
      if (updated != mail) values.set(i, updated);
    }
    if (removed == 0) return 0;
    try {
      dirty.markDirty();
      return removed;
    } catch (RuntimeException failure) {
      values.clear();
      values.addAll(before);
      throw failure;
    }
  }

  /** Creates compatibility metadata for legacy DeliveryBox entries not referenced by any personal mail. */
  public synchronized int adoptLegacy(
      UUID ownerId, List<DeliveryBoxEntrySnapshot> deliveries, long nowEpochMillis, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(deliveries, "deliveries");
    Objects.requireNonNull(dirty, "dirty");
    Set<UUID> referenced = referencedAttachmentIds(ownerId);
    List<MailRecord> additions = new ArrayList<>();
    int existing = personal.getOrDefault(ownerId, List.of()).size();
    for (DeliveryBoxEntrySnapshot entry : deliveries) {
      if (referenced.contains(entry.entryId())) continue;
      if (existing + additions.size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) break;
      String source = entry.source();
      MailType type = source.toLowerCase(java.util.Locale.ROOT).contains("market")
          ? MailType.MARKET : MailType.SYSTEM;
      additions.add(new MailRecord(UUID.randomUUID(), type, null, "", "", "", source,
          Math.max(0, nowEpochMillis), 0, List.of(entry.entryId()), false, true));
    }
    if (additions.isEmpty()) return 0;
    List<MailRecord> values = personal.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    int start = values.size();
    values.addAll(additions);
    try {
      dirty.markDirty();
      return additions.size();
    } catch (RuntimeException failure) {
      values.subList(start, values.size()).clear();
      if (values.isEmpty()) personal.remove(ownerId);
      throw failure;
    }
  }

  /** Removes expired text-only personal mail and expired global announcements. Attachments never expire silently. */
  public synchronized int purgeExpired(long nowEpochMillis, DirtyMarker dirty) {
    Objects.requireNonNull(dirty, "dirty");
    State before = snapshot();
    int removed = 0;
    for (var iterator = personal.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry<UUID, List<MailRecord>> box = iterator.next();
      int beforeSize = box.getValue().size();
      box.getValue().removeIf(mail -> mail.expired(nowEpochMillis) && !mail.hasUnclaimedAttachments());
      removed += beforeSize - box.getValue().size();
      if (box.getValue().isEmpty()) iterator.remove();
    }
    int beforeAnnouncements = announcements.size();
    Set<UUID> expiredAnnouncementIds = new HashSet<>();
    for (MailRecord mail : announcements) if (mail.expired(nowEpochMillis)) expiredAnnouncementIds.add(mail.mailId());
    announcements.removeIf(mail -> expiredAnnouncementIds.contains(mail.mailId()));
    removed += beforeAnnouncements - announcements.size();
    if (!expiredAnnouncementIds.isEmpty()) {
      announcementReads.values().forEach(values -> values.removeAll(expiredAnnouncementIds));
      announcementDismissed.values().forEach(values -> values.removeAll(expiredAnnouncementIds));
      announcementReads.entrySet().removeIf(entry -> entry.getValue().isEmpty());
      announcementDismissed.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    if (removed == 0) return 0;
    try {
      dirty.markDirty();
      return removed;
    } catch (RuntimeException failure) {
      restore(before);
      throw failure;
    }
  }

  public synchronized Set<UUID> referencedAttachmentIds(UUID ownerId) {
    Set<UUID> ids = new LinkedHashSet<>();
    for (MailRecord mail : personal.getOrDefault(ownerId, List.of())) ids.addAll(mail.attachmentIds());
    return Set.copyOf(ids);
  }

  public synchronized MailRecord findPersonal(UUID ownerId, UUID mailId) {
    for (MailRecord mail : personal.getOrDefault(ownerId, List.of())) {
      if (mail.mailId().equals(mailId)) return mail;
    }
    return null;
  }

  public synchronized State snapshot() {
    Map<UUID, List<MailRecord>> personalCopy = new LinkedHashMap<>();
    personal.forEach((owner, values) -> personalCopy.put(owner, List.copyOf(values)));
    return new State(Map.copyOf(personalCopy), List.copyOf(announcements), copySets(announcementReads),
        copySets(announcementDismissed));
  }

  public synchronized void restore(State state) {
    Objects.requireNonNull(state, "state");
    personal.clear();
    announcements.clear();
    announcementReads.clear();
    announcementDismissed.clear();
    for (Map.Entry<UUID, List<MailRecord>> entry : state.personal().entrySet()) {
      Objects.requireNonNull(entry.getKey(), "ownerId");
      List<MailRecord> values = List.copyOf(entry.getValue());
      if (values.size() > EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
        throw new IllegalArgumentException("mailbox exceeds mail limit");
      }
      ensureUnique(values);
      if (!values.isEmpty()) personal.put(entry.getKey(), new ArrayList<>(values));
    }
    if (state.announcements().size() > EconomyNetworkLimits.MAX_MAIL_ANNOUNCEMENTS) {
      throw new IllegalArgumentException("announcement store exceeds limit");
    }
    ensureUnique(state.announcements());
    for (MailRecord record : state.announcements()) {
      if (record.type() != MailType.ANNOUNCEMENT || !record.attachmentIds().isEmpty()) {
        throw new IllegalArgumentException("invalid global announcement");
      }
      announcements.add(record.withRead(false));
    }
    restoreSets(announcementReads, state.announcementReads());
    restoreSets(announcementDismissed, state.announcementDismissed());
  }

  private MailRecord findAnnouncement(UUID mailId) {
    for (MailRecord record : announcements) if (record.mailId().equals(mailId)) return record;
    return null;
  }

  private static Map<UUID, Set<UUID>> copySets(Map<UUID, Set<UUID>> source) {
    Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
    source.forEach((owner, values) -> {
      if (!values.isEmpty()) result.put(owner, Set.copyOf(values));
    });
    return Map.copyOf(result);
  }

  private static void restoreSets(Map<UUID, Set<UUID>> target, Map<UUID, Set<UUID>> source) {
    source.forEach((owner, values) -> {
      Objects.requireNonNull(owner, "ownerId");
      Set<UUID> copy = new LinkedHashSet<>();
      for (UUID value : values) copy.add(Objects.requireNonNull(value, "mailId"));
      if (!copy.isEmpty()) target.put(owner, copy);
    });
  }

  private static void ensureUnique(List<MailRecord> values) {
    Set<UUID> ids = new HashSet<>();
    for (MailRecord record : values) {
      Objects.requireNonNull(record, "mail");
      if (!ids.add(record.mailId())) throw new IllegalArgumentException("duplicate mail id");
    }
  }

  public record State(
      Map<UUID, List<MailRecord>> personal,
      List<MailRecord> announcements,
      Map<UUID, Set<UUID>> announcementReads,
      Map<UUID, Set<UUID>> announcementDismissed) {
    public State {
      Objects.requireNonNull(personal, "personal");
      Objects.requireNonNull(announcements, "announcements");
      Objects.requireNonNull(announcementReads, "announcementReads");
      Objects.requireNonNull(announcementDismissed, "announcementDismissed");
    }
  }

  public enum MutationResult {
    UPDATED,
    DELETED,
    NO_CHANGE,
    NOT_FOUND,
    HAS_ATTACHMENTS
  }

  @FunctionalInterface
  public interface DirtyMarker {
    void markDirty();
  }
}
