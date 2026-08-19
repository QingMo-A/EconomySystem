package com.mo.economy_system.common.client;

import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxResponseKind;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Client cache for the newest mailbox response. */
public final class ClientMailboxState {
  private static final AtomicReference<Snapshot> CURRENT =
      new AtomicReference<>(new Snapshot(0, -1, MailboxResponseKind.ERROR, List.of()));

  private ClientMailboxState() {}

  public static Snapshot snapshot() { return CURRENT.get(); }

  public static boolean update(MailboxDataResponseMessage message) {
    Objects.requireNonNull(message, "message");
    while (true) {
      Snapshot previous = CURRENT.get();
      if (message.requestId() < previous.requestId()) return false;
      Snapshot next = new Snapshot(nextRevision(previous.revision()), message.requestId(),
          message.kind(), message.mails());
      if (CURRENT.compareAndSet(previous, next)) return true;
    }
  }

  private static long nextRevision(long value) {
    if (value == Long.MAX_VALUE) throw new IllegalStateException("mailbox revision exhausted");
    return value + 1;
  }

  public record Snapshot(long revision, long requestId, MailboxResponseKind kind, List<MailSnapshot> mails) {
    public Snapshot {
      if (revision < 0 || requestId < -1) throw new IllegalArgumentException("invalid mailbox client state");
      Objects.requireNonNull(kind, "kind");
      mails = List.copyOf(Objects.requireNonNull(mails, "mails"));
    }
    public boolean failed() { return kind == MailboxResponseKind.ERROR; }
  }
}
