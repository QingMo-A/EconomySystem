package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/** Persistence port for player-owned commission instances and independent refresh schedules. */
public interface CommissionRepository {
  CommissionPlayerState load(UUID playerId);

  void save(CommissionPlayerState state);

  /**
   * Returns whether a submission packet has already been accepted for this commission.
   *
   * <p>The default implementation is deliberately non-persistent for lightweight test and
   * preview repositories. Target adapters with durable storage should override both methods so
   * retrying a packet after a server restart cannot apply progress twice.</p>
   */
  default boolean hasAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {
    return false;
  }

  /** Records an accepted packet idempotency key in the target's durable store. */
  default void recordAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {}

  default CommissionPlayerState loadOrEmpty(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    CommissionPlayerState state = load(playerId);
    return state == null ? CommissionPlayerState.empty(playerId) : state;
  }
}
