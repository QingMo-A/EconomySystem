package com.mo.economy_system.common.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Allocates monotonic non-negative page request IDs and fails explicitly at exhaustion. */
public final class TerritoryRequestIds {
  private TerritoryRequestIds() {}

  public static long next(AtomicLong sequence) {
    Objects.requireNonNull(sequence, "sequence");
    while (true) {
      long current = sequence.get();
      if (current < 0 || current == Long.MAX_VALUE) {
        throw new IllegalStateException("territory request id exhausted");
      }
      if (sequence.compareAndSet(current, current + 1)) return current;
    }
  }
}
