package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.DeliveryBoxResponseKind;
import java.util.List;

/** Client-only immutable delivery response state used by the Forge UI adapter. */
public final class Forge1201DeliveryBoxClientState {
  private static long requestId = -1;
  private static List<DeliveryBoxEntrySnapshot> entries = List.of();
  private static boolean failed;

  private Forge1201DeliveryBoxClientState() {}

  static synchronized void apply(DeliveryBoxDataResponseMessage message) {
    if (message.requestId() < requestId) return;
    requestId = message.requestId();
    failed = message.kind() == DeliveryBoxResponseKind.ERROR;
    entries = failed ? List.of() : List.copyOf(message.entries());
  }

  public static synchronized Snapshot snapshot() {
    return new Snapshot(requestId, entries, failed);
  }

  public record Snapshot(long requestId, List<DeliveryBoxEntrySnapshot> entries, boolean failed) {
    public Snapshot {
      entries = List.copyOf(entries);
    }
  }
}
