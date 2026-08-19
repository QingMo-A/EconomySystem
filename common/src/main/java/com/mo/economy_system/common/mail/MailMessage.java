package com.mo.economy_system.common.mail;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical mailbox message. Attachments remain handled by the existing delivery
 * claim transaction until the new mailbox persistence layer is enabled.
 */
public record MailMessage(
    UUID id,
    MailType type,
    UUID receiver,
    String sender,
    String subject,
    String body,
    long createdAt,
    boolean read) {

  public MailMessage {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(receiver, "receiver");
    sender = Objects.requireNonNullElse(sender, "");
    subject = Objects.requireNonNullElse(subject, "");
    body = Objects.requireNonNullElse(body, "");
    if (createdAt < 0) throw new IllegalArgumentException("createdAt");
  }

  public MailMessage markRead() {
    return new MailMessage(id, type, receiver, sender, subject, body, createdAt, true);
  }
}
