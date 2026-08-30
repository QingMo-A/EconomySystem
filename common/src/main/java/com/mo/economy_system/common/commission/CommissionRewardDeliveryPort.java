package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/**
 * Target adapter boundary for reward-mail delivery and claim.
 *
 * <p>{@link #deliver(CommissionRewardRecord)} must only create a pending claimable attachment;
 * {@link #claim(UUID, UUID, long)} is the point at which the adapter credits Economy Ledger. Both
 * operations must be idempotent by rewardRecordId.
 */
public interface CommissionRewardDeliveryPort {
  DeliveryResult deliver(CommissionRewardRecord record);

  ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis);

  enum DeliveryResult {
    CREATED,
    ALREADY_DELIVERED,
    RETRYABLE_FAILURE,
    STATE_UNKNOWN
  }

  enum ClaimResult {
    CLAIMED,
    ALREADY_CLAIMED,
    NOT_FOUND,
    WRONG_PLAYER,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  static void validateClaim(UUID rewardRecordId, UUID playerId, long nowMillis) {
    Objects.requireNonNull(rewardRecordId, "rewardRecordId");
    Objects.requireNonNull(playerId, "playerId");
    if (nowMillis <= 0) throw new IllegalArgumentException("nowMillis must be positive");
  }
}
