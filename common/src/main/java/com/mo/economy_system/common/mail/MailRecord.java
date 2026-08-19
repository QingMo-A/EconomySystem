package com.mo.economy_system.common.mail;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Persisted mailbox metadata. Unclaimed item ownership remains authoritative in DeliveryBoxLedger. */
public record MailRecord(
    UUID mailId,
    MailType type,
    UUID senderId,
    String senderName,
    String subject,
    String body,
    String source,
    long createdAtEpochMillis,
    long expiresAtEpochMillis,
    List<UUID> attachmentIds,
    List<MailAttachmentSnapshot> claimedAttachments,
    boolean read,
    boolean protectedMail) {

  /** Backward-compatible constructor for callers that only create unclaimed attachments. */
  public MailRecord(
      UUID mailId,
      MailType type,
      UUID senderId,
      String senderName,
      String subject,
      String body,
      String source,
      long createdAtEpochMillis,
      long expiresAtEpochMillis,
      List<UUID> attachmentIds,
      boolean read,
      boolean protectedMail) {
    this(mailId, type, senderId, senderName, subject, body, source,
        createdAtEpochMillis, expiresAtEpochMillis, attachmentIds, List.of(), read, protectedMail);
  }

  public MailRecord {
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(type, "type");
    senderName = validateText(senderName, EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH, "senderName");
    subject = validateText(subject, EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH, "subject");
    body = validateText(body, EconomyNetworkLimits.MAX_MAIL_BODY_LENGTH, "body");
    source = validateText(source, EconomyNetworkLimits.MAX_MAIL_SOURCE_LENGTH, "source");
    if (createdAtEpochMillis < 0 || expiresAtEpochMillis < 0) {
      throw new IllegalArgumentException("mail timestamps must be non-negative");
    }
    if (expiresAtEpochMillis != 0 && expiresAtEpochMillis < createdAtEpochMillis) {
      throw new IllegalArgumentException("mail expiry predates creation");
    }
    attachmentIds = List.copyOf(Objects.requireNonNull(attachmentIds, "attachmentIds"));
    claimedAttachments = List.copyOf(Objects.requireNonNull(claimedAttachments, "claimedAttachments"));
    if (attachmentIds.size() > EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) {
      throw new IllegalArgumentException("mail has too many attachments");
    }
    HashSet<UUID> unique = new HashSet<>();
    for (UUID id : attachmentIds) {
      Objects.requireNonNull(id, "attachmentId");
      if (!unique.add(id)) throw new IllegalArgumentException("duplicate mail attachment id");
    }
    HashSet<UUID> claimedIds = new HashSet<>();
    for (MailAttachmentSnapshot attachment : claimedAttachments) {
      Objects.requireNonNull(attachment, "claimedAttachment");
      if (!attachment.claimed()) throw new IllegalArgumentException("claimed attachment must be marked claimed");
      if (!unique.contains(attachment.entryId())) throw new IllegalArgumentException("claimed attachment is not part of mail");
      if (!claimedIds.add(attachment.entryId())) throw new IllegalArgumentException("duplicate claimed attachment id");
    }
  }

  public boolean expired(long nowEpochMillis) {
    return expiresAtEpochMillis != 0 && nowEpochMillis >= expiresAtEpochMillis;
  }

  public List<UUID> unclaimedAttachmentIds() {
    if (claimedAttachments.isEmpty()) return attachmentIds;
    HashSet<UUID> claimedIds = new HashSet<>();
    for (MailAttachmentSnapshot attachment : claimedAttachments) claimedIds.add(attachment.entryId());
    return attachmentIds.stream().filter(id -> !claimedIds.contains(id)).toList();
  }

  public boolean hasUnclaimedAttachments() {
    return !unclaimedAttachmentIds().isEmpty();
  }

  public MailRecord withRead(boolean value) {
    if (read == value) return this;
    return new MailRecord(mailId, type, senderId, senderName, subject, body, source,
        createdAtEpochMillis, expiresAtEpochMillis, attachmentIds, claimedAttachments, value, protectedMail);
  }

  /** Removes an attachment reference without adding claim history (used only for reconciliation/migration cleanup). */
  public MailRecord withoutAttachment(UUID entryId) {
    if (!attachmentIds.contains(entryId)) return this;
    return new MailRecord(mailId, type, senderId, senderName, subject, body, source,
        createdAtEpochMillis, expiresAtEpochMillis,
        attachmentIds.stream().filter(id -> !id.equals(entryId)).toList(),
        claimedAttachments.stream().filter(attachment -> !attachment.entryId().equals(entryId)).toList(),
        read, protectedMail);
  }

  /** Marks one authoritative delivery attachment claimed while preserving its original slot order. */
  public MailRecord withClaimedAttachment(MailAttachmentSnapshot attachment) {
    Objects.requireNonNull(attachment, "attachment");
    if (!attachmentIds.contains(attachment.entryId())) return this;
    if (claimedAttachments.stream().anyMatch(existing -> existing.entryId().equals(attachment.entryId()))) return this;
    List<MailAttachmentSnapshot> history = new ArrayList<>(claimedAttachments);
    history.add(attachment.asClaimed());
    return new MailRecord(mailId, type, senderId, senderName, subject, body, source,
        createdAtEpochMillis, expiresAtEpochMillis,
        attachmentIds, List.copyOf(history), true, protectedMail);
  }

  private static String validateText(String value, int max, String field) {
    String normalized = Objects.requireNonNullElse(value, "");
    if (normalized.length() > max) throw new IllegalArgumentException(field + " exceeds limit");
    return normalized;
  }
}
