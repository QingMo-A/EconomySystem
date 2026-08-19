package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

public record MailboxSendResultMessage(long requestId, MailboxSendStatus status)
    implements EconomyNetworkMessage {
  public MailboxSendResultMessage {
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
    Objects.requireNonNull(status, "status");
  }
}
