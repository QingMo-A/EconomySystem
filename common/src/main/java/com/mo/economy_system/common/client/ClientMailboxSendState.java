package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.MailboxSendResultMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Client cache for the newest player-mail send result. */
public final class ClientMailboxSendState {
  private static final AtomicReference<Snapshot> CURRENT =
      new AtomicReference<>(new Snapshot(0, -1, MailboxSendStatus.FAILED));

  private ClientMailboxSendState() {}

  public static Snapshot snapshot() { return CURRENT.get(); }

  public static boolean update(MailboxSendResultMessage message) {
    Objects.requireNonNull(message, "message");
    while (true) {
      Snapshot previous = CURRENT.get();
      if (message.requestId() < previous.requestId()) return false;
      Snapshot next = new Snapshot(previous.revision() + 1, message.requestId(), message.status());
      if (CURRENT.compareAndSet(previous, next)) return true;
    }
  }

  public record Snapshot(long revision, long requestId, MailboxSendStatus status) {
    public Snapshot {
      if (revision < 0 || requestId < -1) throw new IllegalArgumentException("invalid mailbox send state");
      Objects.requireNonNull(status, "status");
    }
  }
}
