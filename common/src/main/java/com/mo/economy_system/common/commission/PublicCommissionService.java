package com.mo.economy_system.common.commission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative coordinator for public commission progress and one-mail-per-submission. */
public final class PublicCommissionService {
  public enum SubmitOutcome { ACCEPTED, PARTIAL, COMPLETED, EXPIRED, NOT_FOUND, UNAVAILABLE, DUPLICATE, DELIVERY_RETRY }

  public record SubmitResult(SubmitOutcome outcome, PublicCommission commission,
                             int acceptedAmount, int payout, Optional<CommissionRewardRecord> reward,
                             String issue) {
    public SubmitResult {
      Objects.requireNonNull(outcome, "outcome");
      Objects.requireNonNull(commission, "commission");
      Objects.requireNonNull(reward, "reward");
      issue = Objects.requireNonNullElse(issue, "");
    }
  }

  private final PublicCommissionRepository commissions;
  private final CommissionRewardRepository rewards;
  private final CommissionRewardDeliveryPort delivery;

  public PublicCommissionService(PublicCommissionRepository commissions,
                                 CommissionRewardRepository rewards,
                                 CommissionRewardDeliveryPort delivery) {
    this.commissions = Objects.requireNonNull(commissions, "commissions");
    this.rewards = Objects.requireNonNull(rewards, "rewards");
    this.delivery = Objects.requireNonNull(delivery, "delivery");
  }

  public synchronized void create(PublicCommission commission) {
    Objects.requireNonNull(commission, "commission");
    if (commissions.find(commission.commissionId()).isPresent()) {
      throw new IllegalArgumentException("duplicate public commission id");
    }
    commissions.save(commission);
  }

  public synchronized List<PublicCommission> list(long nowMillis) {
    expireDue(nowMillis);
    return commissions.list();
  }

  /** Persists all due public commissions without creating replacement work. */
  public synchronized int expireDue(long nowMillis) {
    if (nowMillis <= 0) throw new IllegalArgumentException("nowMillis must be positive");
    int expired = 0;
    for (PublicCommission value : commissions.list()) {
      PublicCommission updated = value.expireIfDue(nowMillis);
      if (updated != value) {
        commissions.save(updated);
        expired++;
      }
    }
    return expired;
  }

  public synchronized SubmitResult submit(UUID playerId, UUID commissionId, UUID submissionId,
                                           int requestedAmount, long nowMillis) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(commissionId, "commissionId");
    Objects.requireNonNull(submissionId, "submissionId");
    if (nowMillis <= 0) throw new IllegalArgumentException("nowMillis must be positive");
    PublicCommission current = commissions.find(commissionId).orElse(null);
    if (current == null) return missing(commissionId);
    // Scope packet idempotency to the contributing player as well as the commission.  A client
    // UUID is random, but a malicious or buggy client must not be able to consume another
    // player's submission key and turn a valid contribution into a false duplicate.
    String key = "public:" + playerId + ":" + commissionId + ":" + submissionId;
    Optional<CommissionRewardRecord> existingReward = rewards.findByIdempotencyKey(key);
    if (existingReward.isPresent()) {
      return new SubmitResult(SubmitOutcome.DUPLICATE, current, 0, 0, existingReward, "duplicate submission");
    }
    PublicCommission.Submission submission = current.submit(requestedAmount, nowMillis);
    if (!submission.accepted()) {
      commissions.save(submission.commission());
      SubmitOutcome outcome = submission.commission().status() == PublicCommissionStatus.EXPIRED
          ? SubmitOutcome.EXPIRED : SubmitOutcome.UNAVAILABLE;
      return new SubmitResult(outcome, submission.commission(), 0, 0, Optional.empty(), submission.issue());
    }
    commissions.save(submission.commission());
    CommissionRewardRecord reward;
    try {
      reward = rewards.createIfAbsent(new CommissionRewardRecord(
          UUID.randomUUID(), key, playerId, commissionId, "public", current.requesterId(),
          new CommissionRewardSnapshot(CommissionRewardSnapshot.DEFAULT_CURRENCY_ID,
              submission.payout(), "Public commission reward"), nowMillis));
    } catch (RuntimeException persistenceFailure) {
      // The target adapter has not yet been told that the submission is accepted. Restore the
      // shared commission so a retry cannot lose public capacity without a durable reward record.
      // If a repository cannot persist the rollback, its exception is intentionally allowed to
      // surface: silently claiming success would be less safe than failing the packet.
      commissions.save(current);
      throw persistenceFailure;
    }
    CommissionRewardDeliveryPort.DeliveryResult delivered;
    try {
      delivered = delivery.deliver(reward);
    } catch (RuntimeException failure) {
      return new SubmitResult(SubmitOutcome.DELIVERY_RETRY, submission.commission(),
          submission.acceptedAmount(), submission.payout(), Optional.of(reward),
          "reward mail delivery requires retry");
    }
    SubmitOutcome outcome = submission.commission().status() == PublicCommissionStatus.COMPLETED
        ? SubmitOutcome.COMPLETED
        : submission.acceptedAmount() < requestedAmount ? SubmitOutcome.PARTIAL : SubmitOutcome.ACCEPTED;
    if (delivered == CommissionRewardDeliveryPort.DeliveryResult.RETRYABLE_FAILURE
        || delivered == CommissionRewardDeliveryPort.DeliveryResult.STATE_UNKNOWN) {
      outcome = SubmitOutcome.DELIVERY_RETRY;
    }
    return new SubmitResult(outcome, submission.commission(), submission.acceptedAmount(),
        submission.payout(), Optional.of(reward), "");
  }

  public synchronized boolean cancel(UUID commissionId) {
    PublicCommission current = commissions.find(commissionId).orElse(null);
    if (current == null) return false;
    commissions.save(current.cancel());
    return true;
  }

  /** Retries pending mail for partial public submissions without changing shared progress. */
  public synchronized int retryPendingRewards(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    int attempted = 0;
    for (CommissionRewardRecord reward : rewards.listForPlayer(playerId)) {
      if (!"public".equals(reward.batchId())) continue;
      if (reward.status() != CommissionRewardStatus.PENDING_MAIL
          && reward.status() != CommissionRewardStatus.FAILED) continue;
      attempted++;
      try {
        delivery.deliver(reward);
      } catch (RuntimeException ignored) {
        // Isolate a broken mailbox so other pending public rewards still get retried.
      }
    }
    return attempted;
  }

  /** Removes an administrator-owned public commission from persistence. */
  public synchronized boolean remove(UUID commissionId) {
    Objects.requireNonNull(commissionId, "commissionId");
    if (commissions.find(commissionId).isEmpty()) return false;
    commissions.remove(commissionId);
    return true;
  }

  private static SubmitResult missing(UUID commissionId) {
    PublicCommission placeholder = PublicCommission.create(commissionId, "Missing", "system", "System",
        "missing", 1, 1, 1, 2, "").cancel();
    return new SubmitResult(SubmitOutcome.NOT_FOUND, placeholder, 0, 0, Optional.empty(), "commission not found");
  }
}
