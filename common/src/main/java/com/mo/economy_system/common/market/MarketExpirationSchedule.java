package com.mo.economy_system.common.market;

/** Shared cadence for scanning persisted market orders after their real-time deadline. */
public final class MarketExpirationSchedule {
  public static final int INTERVAL_TICKS = 100;

  private MarketExpirationSchedule() {}

  public static boolean shouldRun(long serverTick) {
    return serverTick >= 0 && serverTick % INTERVAL_TICKS == 0;
  }
}
