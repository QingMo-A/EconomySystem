package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record MailboxClaimAttachmentMessage(UUID mailId, UUID entryId, long requestId)
    implements EconomyNetworkMessage {
  public MailboxClaimAttachmentMessage {
    Objects.requireNonNull(mailId, "mailId");
    Objects.requireNonNull(entryId, "entryId");
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
  }
}
