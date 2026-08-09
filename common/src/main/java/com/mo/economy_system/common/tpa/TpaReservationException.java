package com.mo.economy_system.common.tpa;

/** Explicit reservation failure; rollbackFailed means inventory state is not safe to retry. */
public final class TpaReservationException extends Exception {
  private final int slot;
  private final boolean rollbackFailed;

  public TpaReservationException(int slot, boolean rollbackFailed, Exception cause) {
    super("wormhole-potion reservation failed at slot " + slot, cause);
    this.slot = slot;
    this.rollbackFailed = rollbackFailed;
  }

  public int slot() { return slot; }
  public boolean rollbackFailed() { return rollbackFailed; }
}
