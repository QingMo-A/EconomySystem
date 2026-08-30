package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/** Per-player absolute refresh clock. It is independent from every instance's expiry clock. */
public record PersonalCommissionSchedule(UUID playerId, long nextRefreshAt, long lastRefreshAt) {
  public PersonalCommissionSchedule {
    Objects.requireNonNull(playerId, "playerId");
    if (lastRefreshAt < 0 || nextRefreshAt <= lastRefreshAt) {
      throw new IllegalArgumentException("invalid refresh schedule");
    }
  }

  public static PersonalCommissionSchedule initial(
      UUID playerId, long nowMillis, long intervalMillis) {
    if (nowMillis < 0 || intervalMillis <= 0) throw new IllegalArgumentException("invalid initial schedule");
    return new PersonalCommissionSchedule(playerId, Math.addExact(nowMillis, intervalMillis), nowMillis);
  }

  public boolean due(long nowMillis) {
    return nowMillis >= nextRefreshAt;
  }

  public PersonalCommissionSchedule reschedule(long refreshedAt, long nextRefreshAt) {
    return new PersonalCommissionSchedule(playerId, nextRefreshAt, refreshedAt);
  }
}
