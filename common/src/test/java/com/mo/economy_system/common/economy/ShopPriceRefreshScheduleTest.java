package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShopPriceRefreshScheduleTest {
  @Test
  void refreshesTwicePerDayOncePerTickAndHandlesTimeRollback() {
    ShopPriceRefreshSchedule schedule = new ShopPriceRefreshSchedule();
    assertFalse(schedule.shouldRefresh(5_999));
    assertTrue(schedule.shouldRefresh(6_000));
    assertFalse(schedule.shouldRefresh(6_000));
    assertFalse(schedule.shouldRefresh(17_999));
    assertTrue(schedule.shouldRefresh(18_000));
    assertTrue(schedule.shouldRefresh(30_000));

    assertFalse(schedule.shouldRefresh(5_999));
    assertTrue(schedule.shouldRefresh(6_000));
    schedule.reset();
    assertTrue(schedule.shouldRefresh(6_000));
  }
}
