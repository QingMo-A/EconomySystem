package com.mo.economy_system.common.network;

import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

/** Lightweight S2C event used to surface a newly delivered mailbox entry as a client toast. */
public record MailboxNotificationMessage(MailType type, String senderName, String subject)
    implements EconomyNetworkMessage {
  public MailboxNotificationMessage {
    type = Objects.requireNonNull(type, "type");
    senderName = Objects.requireNonNullElse(senderName, "");
    subject = Objects.requireNonNullElse(subject, "");
    if (senderName.length() > EconomyNetworkLimits.MAX_MAIL_SENDER_LENGTH) {
      throw new IllegalArgumentException("senderName exceeds limit");
    }
    if (subject.length() > EconomyNetworkLimits.MAX_MAIL_SUBJECT_LENGTH) {
      throw new IllegalArgumentException("subject exceeds limit");
    }
  }
}
