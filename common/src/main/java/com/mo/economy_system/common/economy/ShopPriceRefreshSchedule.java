package com.mo.economy_system.common.economy;

/** Edge-triggered twice-per-Minecraft-day shop refresh schedule. */
public final class ShopPriceRefreshSchedule {
  public static final String REFRESH_MESSAGE_KEY = "message.shop.shop_refresh";
  private static final long HALF_DAY_TICKS = 12_000L;
  private static final long FIRST_REFRESH_TICK = 6_000L;

  private long lastObservedDayTime = Long.MIN_VALUE;
  private long lastRefreshDayTime = Long.MIN_VALUE;

  public synchronized boolean shouldRefresh(long absoluteDayTime) {
    if (lastObservedDayTime != Long.MIN_VALUE && absoluteDayTime < lastObservedDayTime) {
      lastRefreshDayTime = Long.MIN_VALUE;
    }
    lastObservedDayTime = absoluteDayTime;
    if (Math.floorMod(absoluteDayTime - FIRST_REFRESH_TICK, HALF_DAY_TICKS) != 0) return false;
    if (absoluteDayTime == lastRefreshDayTime) return false;
    lastRefreshDayTime = absoluteDayTime;
    return true;
  }

  public synchronized void reset() {
    lastObservedDayTime = Long.MIN_VALUE;
    lastRefreshDayTime = Long.MIN_VALUE;
  }
}
