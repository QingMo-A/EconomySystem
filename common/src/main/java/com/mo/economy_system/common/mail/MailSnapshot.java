package com.mo.economy_system.common.mail;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Client-facing mailbox mail with resolved attachment state and per-player read state. */
public record MailSnapshot(
    UUID mailId,
    MailType type,
    UUID senderId,
    String senderName,
    String subject,
    String body,
    String source,
    long createdAtEpochMillis,
    long expiresAtEpochMillis,
    boolean read,
    boolean globalAnnouncement,
    boolean protectedMail,
    List<MailAttachmentSnapshot> attachments,
    int moneyAmount) {

  /** Backward-compatible constructor for snapshots without a monetary transfer. */
  public MailSnapshot(
      UUID mailId,
      MailType type,
      UUID senderId,
      String senderName,
      String subject,
      String body,
      String source,
      long createdAtEpochMillis,
      long expiresAtEpochMillis,
      boolean read,
      boolean globalAnnouncement,
      boolean protectedMail,
      List<MailAttachmentSnapshot> attachments) {
    this(mailId, type, senderId, senderName, subject, body, source, createdAtEpochMillis,
        expiresAtEpochMillis, read, globalAnnouncement, protectedMail, attachments, 0);
  }

  public MailSnapshot {
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(type, "type");
    senderName = Objects.requireNonNullElse(senderName, "");
    subject = Objects.requireNonNullElse(subject, "");
    body = Objects.requireNonNullElse(body, "");
    source = Objects.requireNonNullElse(source, "");
    attachments = List.copyOf(Objects.requireNonNull(attachments, "attachments"));
    if (moneyAmount < 0) throw new IllegalArgumentException("moneyAmount must be non-negative");
  }

  public boolean hasAttachments() {
    return !attachments.isEmpty();
  }

  public boolean hasMoney() {
    return moneyAmount > 0;
  }

  public boolean hasUnclaimedAttachments() {
    return attachments.stream().anyMatch(attachment -> !attachment.claimed());
  }

  public int unclaimedAttachmentCount() {
    return (int) attachments.stream().filter(attachment -> !attachment.claimed()).count();
  }

  public boolean expired(long nowEpochMillis) {
    return expiresAtEpochMillis != 0 && nowEpochMillis >= expiresAtEpochMillis;
  }
}
