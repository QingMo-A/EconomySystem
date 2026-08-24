package com.mo.economy_system.common.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Allocates monotonic non-negative territory request IDs and fails explicitly at exhaustion. */
public final class TerritoryRequestIds {
  private static final AtomicLong SINGLE_TERRITORY_SEQUENCE = new AtomicLong();

  private TerritoryRequestIds() {}

  /**
   * Allocates from the one sequence shared by every screen that consumes the
   * single-territory response cache. Independent per-screen sequences can
   * otherwise make a valid response look older than a response from the
   * previously open screen.
   */
  public static long nextSingleTerritory() {
    return next(SINGLE_TERRITORY_SEQUENCE);
  }

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
