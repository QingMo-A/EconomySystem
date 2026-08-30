package com.mo.economy_system.common.commission;

/** Server-authoritative lifecycle of one personal commission instance. */
public enum CommissionStatus {
  AVAILABLE,
  ACTIVE,
  COMPLETED,
  EXPIRED,
  DISABLED,
  LOCKED;

  public boolean terminal() {
    return this == COMPLETED || this == EXPIRED || this == DISABLED;
  }

  /** Whether this instance consumes the player's active-commission quota. */
  public boolean countsAsActive() {
    return this == AVAILABLE || this == ACTIVE || this == LOCKED;
  }
}
