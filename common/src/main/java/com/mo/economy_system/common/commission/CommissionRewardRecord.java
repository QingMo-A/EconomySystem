package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/**
 * Auditable, idempotent record for one commission reward.
 *
 * <p>It is intentionally not a {@code MailRecord}: a target adapter must create a claimable mail
 * currency attachment from this record and credit the account only when that attachment is claimed.
 */
public record CommissionRewardRecord(
    UUID rewardRecordId,
    String idempotencyKey,
    UUID playerId,
    UUID commissionId,
    String batchId,
    String templateId,
    String requesterId,
    CommissionRewardSnapshot rewardSnapshot,
    long createdAt,
    UUID mailId,
    CommissionRewardStatus status,
    long claimedAt) {

  public CommissionRewardRecord {
    Objects.requireNonNull(rewardRecordId, "rewardRecordId");
    idempotencyKey = text(idempotencyKey, "idempotencyKey", 128);
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(commissionId, "commissionId");
    batchId = Objects.requireNonNullElse(batchId, "");
    if (batchId.length() > 128) throw new IllegalArgumentException("batchId exceeds limit");
    templateId = identifier(templateId, "templateId");
    requesterId = identifier(requesterId, "requesterId");
    Objects.requireNonNull(rewardSnapshot, "rewardSnapshot");
    if (createdAt < 0) throw new IllegalArgumentException("createdAt must be non-negative");
    Objects.requireNonNull(status, "status");
    if (claimedAt < 0) throw new IllegalArgumentException("claimedAt must be non-negative");
    if (status == CommissionRewardStatus.CLAIMED && claimedAt == 0) {
      throw new IllegalArgumentException("claimed reward must have claimedAt");
    }
    if (status != CommissionRewardStatus.CLAIMED && claimedAt != 0) {
      throw new IllegalArgumentException("only claimed reward may have claimedAt");
    }
    if (status == CommissionRewardStatus.MAIL_CREATED && mailId == null) {
      throw new IllegalArgumentException("mail-created reward must have mailId");
    }
    if (status == CommissionRewardStatus.CLAIMED && mailId == null) {
      throw new IllegalArgumentException("claimed reward must have mailId");
    }
  }

  public CommissionRewardRecord(
      UUID rewardRecordId,
      String idempotencyKey,
      UUID playerId,
      UUID commissionId,
      String templateId,
      String requesterId,
      CommissionRewardSnapshot rewardSnapshot,
      long createdAt) {
    this(rewardRecordId, idempotencyKey, playerId, commissionId, "", templateId, requesterId,
        rewardSnapshot, createdAt, null, CommissionRewardStatus.PENDING_MAIL, 0);
  }

  public int currencyAmount() {
    return rewardSnapshot.amount();
  }

  public int currencyRewardAmount() {
    return rewardSnapshot.amount();
  }

  public boolean currencyRewardClaimed() {
    return status == CommissionRewardStatus.CLAIMED;
  }

  public CommissionRewardRecord mailCreated(UUID createdMailId) {
    Objects.requireNonNull(createdMailId, "mailId");
    if (status == CommissionRewardStatus.MAIL_CREATED || status == CommissionRewardStatus.CLAIMED) {
      if (!createdMailId.equals(mailId)) throw new IllegalStateException("reward mail identity mismatch");
      return this;
    }
    if (status != CommissionRewardStatus.PENDING_MAIL && status != CommissionRewardStatus.FAILED) {
      throw new IllegalStateException("reward cannot create mail from " + status);
    }
    return new CommissionRewardRecord(rewardRecordId, idempotencyKey, playerId, commissionId, batchId,
        templateId, requesterId, rewardSnapshot, createdAt, createdMailId,
        CommissionRewardStatus.MAIL_CREATED, 0);
  }

  public CommissionRewardRecord claimed(long claimedAtMillis) {
    if (status == CommissionRewardStatus.CLAIMED) return this;
    if (status != CommissionRewardStatus.MAIL_CREATED || mailId == null) {
      throw new IllegalStateException("reward is not claimable: " + status);
    }
    if (claimedAtMillis <= 0) throw new IllegalArgumentException("claimedAtMillis must be positive");
    return new CommissionRewardRecord(rewardRecordId, idempotencyKey, playerId, commissionId, batchId,
        templateId, requesterId, rewardSnapshot, createdAt, mailId,
        CommissionRewardStatus.CLAIMED, claimedAtMillis);
  }

  public CommissionRewardRecord failed() {
    if (status == CommissionRewardStatus.CLAIMED) return this;
    return new CommissionRewardRecord(rewardRecordId, idempotencyKey, playerId, commissionId, batchId,
        templateId, requesterId, rewardSnapshot, createdAt, null, CommissionRewardStatus.FAILED, 0);
  }

  private static String text(String value, String field, int maxLength) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) throw new IllegalArgumentException(field + " is invalid");
    return normalized;
  }

  private static String identifier(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > 64 || !normalized.matches("[a-zA-Z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }
}
