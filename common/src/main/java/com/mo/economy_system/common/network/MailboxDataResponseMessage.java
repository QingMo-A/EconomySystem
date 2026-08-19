package com.mo.economy_system.common.network;

import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.List;
import java.util.Objects;

public record MailboxDataResponseMessage(
    MailboxResponseKind kind, long requestId, List<MailSnapshot> mails)
    implements EconomyNetworkMessage {
  public MailboxDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
    mails = List.copyOf(Objects.requireNonNull(mails, "mails"));
    if (mails.size() > EconomyNetworkLimits.MAX_MAILS_PER_PLAYER + EconomyNetworkLimits.MAX_MAIL_ANNOUNCEMENTS) {
      throw new IllegalArgumentException("too many mailbox mails");
    }
    if (kind == MailboxResponseKind.ERROR && !mails.isEmpty()) {
      throw new IllegalArgumentException("error mailbox response cannot contain mail");
    }
  }

  public static MailboxDataResponseMessage data(long requestId, List<MailSnapshot> mails) {
    return new MailboxDataResponseMessage(MailboxResponseKind.DATA, requestId, mails);
  }

  public static MailboxDataResponseMessage error(long requestId) {
    return new MailboxDataResponseMessage(MailboxResponseKind.ERROR, requestId, List.of());
  }
}
