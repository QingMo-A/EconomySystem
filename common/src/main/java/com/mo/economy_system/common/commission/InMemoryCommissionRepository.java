package com.mo.economy_system.common.commission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Small deterministic repository useful for tests and target adapters' initial integration. */
public final class InMemoryCommissionRepository implements CommissionRepository, CommissionRewardRepository {
  private final Map<UUID, CommissionPlayerState> players = new LinkedHashMap<>();
  private final Map<UUID, CommissionRewardRecord> rewards = new LinkedHashMap<>();
  private final Map<String, UUID> rewardKeys = new LinkedHashMap<>();
  private final java.util.Set<String> acceptedSubmissionKeys = new java.util.HashSet<>();

  @Override
  public synchronized CommissionPlayerState load(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    CommissionPlayerState state = players.get(playerId);
    return state == null ? null : new CommissionPlayerState(state.playerId(), state.commissions(), state.schedule());
  }

  @Override
  public synchronized void save(CommissionPlayerState state) {
    Objects.requireNonNull(state, "state");
    players.put(state.playerId(), new CommissionPlayerState(state.playerId(), state.commissions(), state.schedule()));
  }

  @Override
  public synchronized boolean hasAcceptedSubmission(UUID playerId, UUID commissionId,
      UUID submissionId) {
    return acceptedSubmissionKeys.contains(submissionKey(playerId, commissionId, submissionId));
  }

  @Override
  public synchronized void recordAcceptedSubmission(UUID playerId, UUID commissionId,
      UUID submissionId) {
    acceptedSubmissionKeys.add(submissionKey(playerId, commissionId, submissionId));
  }

  private static String submissionKey(UUID playerId, UUID commissionId, UUID submissionId) {
    return Objects.requireNonNull(playerId, "playerId") + ":"
        + Objects.requireNonNull(commissionId, "commissionId") + ":"
        + Objects.requireNonNull(submissionId, "submissionId");
  }

  @Override
  public synchronized Optional<CommissionRewardRecord> find(UUID rewardRecordId) {
    return Optional.ofNullable(rewards.get(Objects.requireNonNull(rewardRecordId, "rewardRecordId")));
  }

  @Override
  public synchronized Optional<CommissionRewardRecord> findByIdempotencyKey(String idempotencyKey) {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    UUID id = rewardKeys.get(idempotencyKey);
    return id == null ? Optional.empty() : Optional.ofNullable(rewards.get(id));
  }

  @Override
  public synchronized CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) {
    Objects.requireNonNull(candidate, "candidate");
    UUID existingId = rewardKeys.get(candidate.idempotencyKey());
    if (existingId != null) return rewards.get(existingId);
    if (rewards.containsKey(candidate.rewardRecordId())) {
      throw new IllegalArgumentException("duplicate rewardRecordId");
    }
    rewards.put(candidate.rewardRecordId(), candidate);
    rewardKeys.put(candidate.idempotencyKey(), candidate.rewardRecordId());
    return candidate;
  }

  @Override
  public synchronized void save(CommissionRewardRecord record) {
    Objects.requireNonNull(record, "record");
    UUID existingId = rewardKeys.get(record.idempotencyKey());
    if (existingId != null && !existingId.equals(record.rewardRecordId())) {
      throw new IllegalArgumentException("duplicate reward idempotency key");
    }
    rewards.put(record.rewardRecordId(), record);
    rewardKeys.put(record.idempotencyKey(), record.rewardRecordId());
  }

  @Override
  public synchronized List<CommissionRewardRecord> listForPlayer(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    List<CommissionRewardRecord> result = new ArrayList<>();
    for (CommissionRewardRecord record : rewards.values()) {
      if (record.playerId().equals(playerId)) result.add(record);
    }
    return List.copyOf(result);
  }
}
