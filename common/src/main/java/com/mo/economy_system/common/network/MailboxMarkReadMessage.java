package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record MailboxMarkReadMessage(UUID mailId, long requestId) implements EconomyNetworkMessage {
  public MailboxMarkReadMessage {
    Objects.requireNonNull(mailId, "mailId");
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
  }
}
