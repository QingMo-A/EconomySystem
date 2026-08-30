package com.mo.economy_system.common.commission;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Loader-neutral orchestration for personal commissions.
 *
 * <p>The service owns schedule refresh, progress transitions and creation of an auditable reward
 * record. It deliberately does not know how items, entities, accounts or mailboxes are represented;
 * target adapters validate those facts and call {@link #submitProgress(UUID, UUID, int, long)}.
 */
public final class CommissionService {
  public enum SubmitOutcome {
    PROGRESSED,
    COMPLETED,
    ALREADY_COMPLETED,
    EXPIRED,
    DISABLED,
    NOT_FOUND,
    INVALID_AMOUNT,
    REWARD_PENDING_MAIL,
    REWARD_DELIVERY_RETRY,
    DUPLICATE_SUBMISSION
  }

  public record RefreshView(
      CommissionGenerator.RefreshResult generation,
      CommissionPlayerState state) {
    public RefreshView {
      Objects.requireNonNull(generation, "generation");
      Objects.requireNonNull(state, "state");
    }
  }

  public record SubmitResult(
      SubmitOutcome outcome,
      CommissionInstance commission,
      Optional<CommissionRewardRecord> reward,
      String issue) {
    public SubmitResult {
      Objects.requireNonNull(outcome, "outcome");
      Objects.requireNonNull(commission, "commission");
      Objects.requireNonNull(reward, "reward");
      issue = Objects.requireNonNullElse(issue, "");
    }

    public boolean accepted() {
      return outcome == SubmitOutcome.PROGRESSED || outcome == SubmitOutcome.COMPLETED
          || outcome == SubmitOutcome.REWARD_PENDING_MAIL
          || outcome == SubmitOutcome.REWARD_DELIVERY_RETRY;
    }
  }

  private final CommissionGenerator generator;
  private final CommissionRepository commissions;
  private final CommissionRewardRepository rewards;
  private final CommissionRewardDeliveryPort delivery;
  /**
   * Packet-level idempotency guard. Target persistence still makes completed rewards durable;
   * this bounded session cache prevents a retried partial-progress packet from being applied twice.
   */
  private final Set<String> acceptedSubmissions = new LinkedHashSet<>();
  private static final int MAX_REMEMBERED_SUBMISSIONS = 8_192;

  public CommissionService(
      CommissionGenerator generator,
      CommissionRepository commissions,
      CommissionRewardRepository rewards,
      CommissionRewardDeliveryPort delivery) {
    this.generator = Objects.requireNonNull(generator, "generator");
    this.commissions = Objects.requireNonNull(commissions, "commissions");
    this.rewards = Objects.requireNonNull(rewards, "rewards");
    this.delivery = Objects.requireNonNull(delivery, "delivery");
  }

  /** Refreshes a player if their absolute server-time schedule is due. */
  public synchronized RefreshView refresh(UUID playerId, long nowMillis) {
    return refreshInternal(playerId, nowMillis, false);
  }

  /**
   * Forces an administrator refresh without deleting or replacing existing work.
   *
   * <p>The player's next scheduled refresh is advanced from this operation so a forced refresh
   * cannot be followed immediately by an automatic duplicate refresh.  Capacity and catalog
   * legality rules still apply.
   */
  public synchronized RefreshView forceRefresh(UUID playerId, long nowMillis) {
    return refreshInternal(playerId, nowMillis, true);
  }

  private RefreshView refreshInternal(UUID playerId, long nowMillis, boolean force) {
    requireTime(nowMillis);
    CommissionPlayerState before = commissions.loadOrEmpty(playerId);
    PersonalCommissionSchedule schedule = before.schedule();
    if (schedule == null) {
      // A first login should receive work immediately.  Use a one-millisecond due schedule
      // instead of subtracting the interval: a newly-created world can have a tiny synthetic
      // clock in tests (or during startup), and clamping that subtraction would otherwise defer
      // the first commission for the entire refresh interval.
      long firstLastRefresh = Math.max(0L, nowMillis - 1L);
      schedule = new PersonalCommissionSchedule(playerId, nowMillis, firstLastRefresh);
    }
    CommissionGenerator.RefreshResult result = force
        ? generator.forceRefresh(playerId, nowMillis, before.commissions(), schedule)
        : generator.refresh(playerId, nowMillis, before.commissions(), schedule);
    CommissionPlayerState after = new CommissionPlayerState(
        playerId, result.current(), result.schedule());
    if (!after.equals(before)) commissions.save(after);
    return new RefreshView(result, after);
  }

  /**
   * Retries pending personal reward-mail delivery for one player.  This is intentionally separate
   * from progress submission so a completed commission can recover even when the player never
   * opens the UI or sends another packet.
   */
  public synchronized int retryPendingRewards(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    int attempted = 0;
    for (CommissionRewardRecord reward : rewards.listForPlayer(playerId)) {
      if (!reward.batchId().isBlank()) continue;
      if (reward.status() != CommissionRewardStatus.PENDING_MAIL
          && reward.status() != CommissionRewardStatus.FAILED) continue;
      attempted++;
      try {
        delivery.deliver(reward);
      } catch (RuntimeException ignored) {
        // Keep retrying the remaining records; one broken mailbox must not starve other rewards.
      }
    }
    return attempted;
  }

  /** Generates administrator-requested preview work without advancing the player's schedule. */
  public synchronized List<CommissionInstance> generate(
      UUID playerId, long nowMillis, int count) {
    requireTime(nowMillis);
    CommissionPlayerState state = commissions.loadOrEmpty(playerId);
    List<CommissionInstance> additions = generator.generate(
        playerId, nowMillis, count, state.commissions());
    if (!additions.isEmpty()) {
      List<CommissionInstance> merged = new ArrayList<>(state.commissions());
      merged.addAll(additions);
      commissions.save(state.withCommissions(merged));
    }
    return additions;
  }

  /**
   * Applies server-validated progress. Completion is persisted before delivery is attempted, so a
   * mailbox outage leaves a completed task and a retryable reward record rather than losing work.
   */
  public synchronized SubmitResult submitProgress(
      UUID playerId, UUID commissionId, int amount, long nowMillis) {
    return submitProgress(playerId, commissionId, UUID.randomUUID(), amount, nowMillis);
  }

  /** Applies progress with an explicit packet idempotency key. */
  public synchronized SubmitResult submitProgress(
      UUID playerId, UUID commissionId, UUID submissionId, int amount, long nowMillis) {
    requireTime(nowMillis);
    Objects.requireNonNull(submissionId, "submissionId");
    if (amount <= 0) {
      return new SubmitResult(SubmitOutcome.INVALID_AMOUNT,
          placeholder(playerId, commissionId), Optional.empty(), "amount must be positive");
    }
    CommissionPlayerState state = commissions.loadOrEmpty(playerId);
    String submissionKey = playerId + ":" + commissionId + ":" + submissionId;
    if (acceptedSubmissions.contains(submissionKey)
        || commissions.hasAcceptedSubmission(playerId, commissionId, submissionId)) {
      CommissionInstance duplicate = state.commissions().stream()
          .filter(value -> value.commissionId().equals(commissionId)).findFirst()
          .orElse(placeholder(playerId, commissionId));
      return new SubmitResult(SubmitOutcome.DUPLICATE_SUBMISSION, duplicate, Optional.empty(),
          "duplicate commission submission");
    }
    CommissionInstance found = state.commissions().stream()
        .filter(value -> value.commissionId().equals(commissionId)).findFirst().orElse(null);
    if (found == null) {
      return new SubmitResult(SubmitOutcome.NOT_FOUND,
          placeholder(playerId, commissionId), Optional.empty(), "commission not found");
    }
    if (found.status() == CommissionStatus.COMPLETED) {
      Optional<CommissionRewardRecord> existing = rewardFor(found);
      if (existing.isEmpty() || existing.get().status() == CommissionRewardStatus.CLAIMED
          || existing.get().status() == CommissionRewardStatus.MAIL_CREATED) {
        return new SubmitResult(SubmitOutcome.ALREADY_COMPLETED, found, existing, "");
      }
      CommissionRewardDeliveryPort.DeliveryResult delivered;
      try {
        delivered = delivery.deliver(existing.get());
      } catch (RuntimeException failure) {
        return new SubmitResult(SubmitOutcome.REWARD_DELIVERY_RETRY, found, existing,
            "reward mail delivery requires retry");
      }
      if (delivered == CommissionRewardDeliveryPort.DeliveryResult.CREATED
          || delivered == CommissionRewardDeliveryPort.DeliveryResult.ALREADY_DELIVERED) {
        return new SubmitResult(SubmitOutcome.REWARD_PENDING_MAIL, found,
            rewards.find(existing.get().rewardRecordId()), "");
      }
      return new SubmitResult(SubmitOutcome.REWARD_DELIVERY_RETRY, found, existing,
          "reward mail delivery requires retry");
    }
    if (found.status() == CommissionStatus.DISABLED) {
      return new SubmitResult(SubmitOutcome.DISABLED, found, Optional.empty(), "commission disabled");
    }
    CommissionInstance current = found.expireIfDue(nowMillis);
    if (current.status() == CommissionStatus.EXPIRED) {
      saveReplacing(state, current);
      return new SubmitResult(SubmitOutcome.EXPIRED, current, Optional.empty(), "commission expired");
    }
    CommissionInstance progressed = current.addProgress(amount);
    if (!progressed.completeable()) {
      saveReplacing(state, progressed);
      rememberSubmission(playerId, commissionId, submissionId, submissionKey);
      return new SubmitResult(SubmitOutcome.PROGRESSED, progressed, Optional.empty(), "");
    }

    CommissionInstance completed = progressed.complete();
    saveReplacing(state, completed);
    CommissionRewardRecord candidate = new CommissionRewardRecord(
        UUID.randomUUID(),
        "commission:" + completed.commissionId(),
        playerId,
        completed.commissionId(),
        "",
        completed.templateId(),
        completed.requesterId(),
        completed.rewardSnapshot(),
        nowMillis,
        null,
        CommissionRewardStatus.PENDING_MAIL,
        0);
    CommissionRewardRecord reward = rewards.createIfAbsent(candidate);
    rememberSubmission(playerId, commissionId, submissionId, submissionKey);
    if (reward.status() == CommissionRewardStatus.CLAIMED
        || reward.status() == CommissionRewardStatus.MAIL_CREATED) {
      return new SubmitResult(SubmitOutcome.COMPLETED, completed, Optional.of(reward), "");
    }
    CommissionRewardDeliveryPort.DeliveryResult delivered;
    try {
      delivered = delivery.deliver(reward);
    } catch (RuntimeException failure) {
      return new SubmitResult(SubmitOutcome.REWARD_DELIVERY_RETRY, completed,
          Optional.of(reward), "reward mail delivery requires retry");
    }
    if (delivered == CommissionRewardDeliveryPort.DeliveryResult.CREATED
        || delivered == CommissionRewardDeliveryPort.DeliveryResult.ALREADY_DELIVERED) {
      return new SubmitResult(SubmitOutcome.REWARD_PENDING_MAIL, completed,
          rewards.find(reward.rewardRecordId()), "");
    }
    return new SubmitResult(SubmitOutcome.REWARD_DELIVERY_RETRY, completed,
        Optional.of(reward), "reward mail delivery requires retry");
  }

  private void rememberSubmission(UUID playerId, UUID commissionId, UUID submissionId, String key) {
    acceptedSubmissions.add(key);
    commissions.recordAcceptedSubmission(playerId, commissionId, submissionId);
    while (acceptedSubmissions.size() > MAX_REMEMBERED_SUBMISSIONS) {
      acceptedSubmissions.remove(acceptedSubmissions.iterator().next());
    }
  }

  private Optional<CommissionRewardRecord> rewardFor(CommissionInstance instance) {
    return rewards.findByIdempotencyKey("commission:" + instance.commissionId());
  }

  private void saveReplacing(CommissionPlayerState state, CommissionInstance replacement) {
    List<CommissionInstance> values = new ArrayList<>(state.commissions());
    for (int index = 0; index < values.size(); index++) {
      if (values.get(index).commissionId().equals(replacement.commissionId())) {
        values.set(index, replacement);
        commissions.save(state.withCommissions(values));
        return;
      }
    }
    throw new IllegalStateException("commission disappeared while submitting progress");
  }

  private static void requireTime(long nowMillis) {
    if (nowMillis <= 0) throw new IllegalArgumentException("nowMillis must be positive");
  }

  private static CommissionInstance placeholder(UUID playerId, UUID commissionId) {
    return new CommissionInstance(
        commissionId == null ? UUID.randomUUID() : commissionId,
        Objects.requireNonNull(playerId, "playerId"),
        "missing",
        CommissionType.CUSTOM,
        "system",
        "system",
        "missing",
        1,
        0,
        CommissionRewardSnapshot.coins(0),
        1,
        2,
        CommissionStatus.DISABLED,
        "");
  }
}
