package com.mo.economy_system.common.commission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable reward-record port. Implementations must make the idempotency-key insert atomic. */
public interface CommissionRewardRepository {
  Optional<CommissionRewardRecord> find(UUID rewardRecordId);

  Optional<CommissionRewardRecord> findByIdempotencyKey(String idempotencyKey);

  /** Returns the existing record for a duplicate key, otherwise persists and returns candidate. */
  CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate);

  void save(CommissionRewardRecord record);

  default List<CommissionRewardRecord> listForPlayer(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return List.of();
  }
}
