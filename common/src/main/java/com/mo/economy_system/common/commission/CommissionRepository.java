package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/** Persistence port for player-owned commission instances and independent refresh schedules. */
public interface CommissionRepository {
  CommissionPlayerState load(UUID playerId);

  void save(CommissionPlayerState state);

  default CommissionPlayerState loadOrEmpty(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    CommissionPlayerState state = load(playerId);
    return state == null ? CommissionPlayerState.empty(playerId) : state;
  }
}
