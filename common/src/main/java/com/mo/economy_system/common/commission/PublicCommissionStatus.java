package com.mo.economy_system.common.commission;

/** Lifecycle of a server-wide, administrator-created commission. */
public enum PublicCommissionStatus {
  AVAILABLE,
  COMPLETED,
  EXHAUSTED,
  EXPIRED,
  CANCELLED,
  DISABLED;

  public boolean terminal() {
    return this != AVAILABLE;
  }
}
