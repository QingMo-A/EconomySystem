package com.mo.economy_system.common.commission;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Persistable per-player commission state; target adapters own the actual storage format. */
public record CommissionPlayerState(
    UUID playerId,
    List<CommissionInstance> commissions,
    PersonalCommissionSchedule schedule) {
  public CommissionPlayerState {
    Objects.requireNonNull(playerId, "playerId");
    commissions = List.copyOf(Objects.requireNonNull(commissions, "commissions"));
    for (CommissionInstance commission : commissions) {
      Objects.requireNonNull(commission, "commission");
      if (!playerId.equals(commission.ownerPlayerId())) throw new IllegalArgumentException("commission owner mismatch");
    }
    if (schedule != null && !playerId.equals(schedule.playerId())) {
      throw new IllegalArgumentException("schedule player mismatch");
    }
  }

  public static CommissionPlayerState empty(UUID playerId) {
    return new CommissionPlayerState(playerId, List.of(), null);
  }

  public CommissionPlayerState withCommissions(List<CommissionInstance> values) {
    return new CommissionPlayerState(playerId, values, schedule);
  }

  public CommissionPlayerState withSchedule(PersonalCommissionSchedule value) {
    return new CommissionPlayerState(playerId, commissions, value);
  }
}
