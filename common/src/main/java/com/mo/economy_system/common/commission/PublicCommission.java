package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot of one administrator-created public commission. */
public record PublicCommission(
    UUID commissionId,
    String name,
    String requesterId,
    String requesterName,
    String targetSnapshot,
    int targetAmount,
    int unitReward,
    long generatedAt,
    long expiresAt,
    String description,
    PublicCommissionStatus status,
    int remainingAmount,
    int remainingBudget) {

  public PublicCommission {
    Objects.requireNonNull(commissionId, "commissionId");
    name = text(name, "name", 128);
    requesterId = identifier(requesterId, "requesterId");
    requesterName = text(requesterName, "requesterName", 128);
    targetSnapshot = text(targetSnapshot, "targetSnapshot", 256);
    if (targetAmount <= 0) throw new IllegalArgumentException("targetAmount must be positive");
    if (unitReward <= 0) throw new IllegalArgumentException("unitReward must be positive");
    if (generatedAt < 0 || expiresAt <= generatedAt) throw new IllegalArgumentException("invalid timestamps");
    description = Objects.requireNonNullElse(description, "");
    if (description.length() > 1024) throw new IllegalArgumentException("description exceeds limit");
    Objects.requireNonNull(status, "status");
    if (remainingAmount < 0 || remainingAmount > targetAmount) {
      throw new IllegalArgumentException("invalid remaining amount");
    }
    long expectedBudget = Math.multiplyExact((long) remainingAmount, unitReward);
    if (expectedBudget > Integer.MAX_VALUE || remainingBudget != expectedBudget) {
      throw new IllegalArgumentException("remaining budget does not match remaining amount");
    }
    if (status == PublicCommissionStatus.COMPLETED && remainingAmount != 0) {
      throw new IllegalArgumentException("completed commission must be exhausted");
    }
  }

  public static PublicCommission create(
      UUID id, String name, String requesterId, String requesterName, String target,
      int amount, int unitReward, long generatedAt, long expiresAt, String description) {
    long budget = Math.multiplyExact((long) amount, unitReward);
    if (budget > Integer.MAX_VALUE) throw new IllegalArgumentException("commission budget overflows");
    return new PublicCommission(id, name, requesterId, requesterName, target, amount, unitReward,
        generatedAt, expiresAt, description, PublicCommissionStatus.AVAILABLE, amount, (int) budget);
  }

  public PublicCommission expireIfDue(long nowMillis) {
    if (status != PublicCommissionStatus.AVAILABLE || nowMillis < expiresAt) return this;
    return new PublicCommission(commissionId, name, requesterId, requesterName, targetSnapshot,
        targetAmount, unitReward, generatedAt, expiresAt, description,
        PublicCommissionStatus.EXPIRED, remainingAmount, remainingBudget);
  }

  public Submission submit(int requestedAmount, long nowMillis) {
    if (requestedAmount <= 0) return Submission.invalid(this, "amount must be positive");
    PublicCommission current = expireIfDue(nowMillis);
    if (current.status == PublicCommissionStatus.EXPIRED) return Submission.invalid(current, "commission expired");
    if (current.status != PublicCommissionStatus.AVAILABLE) return Submission.invalid(current, "commission is not available");
    int accepted = Math.min(requestedAmount, current.remainingAmount);
    if (accepted <= 0) return Submission.invalid(current, "commission is exhausted");
    int payout = Math.multiplyExact(accepted, current.unitReward);
    int left = current.remainingAmount - accepted;
    PublicCommissionStatus nextStatus = left == 0
        ? PublicCommissionStatus.EXHAUSTED : PublicCommissionStatus.AVAILABLE;
    if (left == 0) nextStatus = PublicCommissionStatus.COMPLETED;
    PublicCommission next = new PublicCommission(current.commissionId, current.name, current.requesterId,
        current.requesterName, current.targetSnapshot, current.targetAmount, current.unitReward,
        current.generatedAt, current.expiresAt, current.description, nextStatus, left,
        Math.multiplyExact(left, current.unitReward));
    return new Submission(next, accepted, payout, "");
  }

  public PublicCommission cancel() {
    if (status.terminal()) return this;
    return new PublicCommission(commissionId, name, requesterId, requesterName, targetSnapshot,
        targetAmount, unitReward, generatedAt, expiresAt, description,
        PublicCommissionStatus.CANCELLED, remainingAmount, remainingBudget);
  }

  public record Submission(PublicCommission commission, int acceptedAmount, int payout, String issue) {
    public Submission {
      Objects.requireNonNull(commission, "commission");
      issue = Objects.requireNonNullElse(issue, "");
      if (acceptedAmount < 0 || payout < 0) throw new IllegalArgumentException("invalid submission result");
    }

    public boolean accepted() { return acceptedAmount > 0 && issue.isEmpty(); }

    private static Submission invalid(PublicCommission commission, String issue) {
      return new Submission(commission, 0, 0, issue);
    }
  }

  private static String identifier(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > 64 || !normalized.matches("[a-zA-Z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }

  private static String text(String value, String field, int maxLength) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) throw new IllegalArgumentException(field + " is invalid");
    return normalized;
  }
}
