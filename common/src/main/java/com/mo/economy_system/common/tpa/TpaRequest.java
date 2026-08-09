package com.mo.economy_system.common.tpa;

import java.util.Objects;
import java.util.UUID;

/** Immutable server-session TPA request. */
public record TpaRequest(UUID senderId, UUID targetId, long createdTick, long expiresTick) {
  public TpaRequest {
    Objects.requireNonNull(senderId, "senderId");
    Objects.requireNonNull(targetId, "targetId");
    if (senderId.equals(targetId)) throw new IllegalArgumentException("self request");
    if (createdTick < 0 || expiresTick <= createdTick) throw new IllegalArgumentException("ticks");
  }

  public boolean isExpired(long serverTick) {
    return serverTick >= expiresTick;
  }
}
