package com.mo.economy_system.common.commission;

import java.util.Objects;
import java.util.UUID;

/**
 * One generated personal commission. Every value which can affect a player's quote is copied into
 * this object; changing configuration later cannot rewrite an existing instance.
 */
public record CommissionInstance(
    UUID commissionId,
    UUID ownerPlayerId,
    String templateId,
    CommissionType type,
    String requesterId,
    String requesterName,
    String targetSnapshot,
    int requiredAmount,
    int progress,
    CommissionRewardSnapshot rewardSnapshot,
    long generatedAt,
    long expiresAt,
    CommissionStatus status,
    String text) {

  public CommissionInstance {
    Objects.requireNonNull(commissionId, "commissionId");
    Objects.requireNonNull(ownerPlayerId, "ownerPlayerId");
    templateId = identifier(templateId, "templateId");
    Objects.requireNonNull(type, "type");
    requesterId = identifier(requesterId, "requesterId");
    requesterName = text(requesterName, "requesterName", 128);
    targetSnapshot = text(targetSnapshot, "targetSnapshot", 256);
    if (requiredAmount <= 0) throw new IllegalArgumentException("requiredAmount must be positive");
    if (progress < 0 || progress > requiredAmount) throw new IllegalArgumentException("invalid progress");
    Objects.requireNonNull(rewardSnapshot, "rewardSnapshot");
    if (generatedAt < 0 || expiresAt <= generatedAt) throw new IllegalArgumentException("invalid commission timestamps");
    Objects.requireNonNull(status, "status");
    text = Objects.requireNonNullElse(text, "");
    if (text.length() > 1024) throw new IllegalArgumentException("text exceeds limit");
    if (status == CommissionStatus.COMPLETED && progress < requiredAmount) {
      throw new IllegalArgumentException("completed commission must be complete");
    }
  }

  public CommissionInstance(
      UUID commissionId,
      UUID ownerPlayerId,
      String templateId,
      CommissionType type,
      String requesterId,
      String requesterName,
      String targetSnapshot,
      int requiredAmount,
      CommissionRewardSnapshot rewardSnapshot,
      long generatedAt,
      long expiresAt) {
    this(
        commissionId,
        ownerPlayerId,
        templateId,
        type,
        requesterId,
        requesterName,
        targetSnapshot,
        requiredAmount,
        0,
        rewardSnapshot,
        generatedAt,
        expiresAt,
        CommissionStatus.AVAILABLE,
        "");
  }

  public boolean isExpired(long nowMillis) {
    return nowMillis >= expiresAt && !status.terminal();
  }

  public boolean completeable() {
    return !status.terminal() && progress >= requiredAmount;
  }

  public boolean completed() {
    return status == CommissionStatus.COMPLETED;
  }

  public CommissionInstance activate() {
    if (status == CommissionStatus.AVAILABLE) return copyWith(progress, CommissionStatus.ACTIVE);
    return this;
  }

  public CommissionInstance withProgress(int newProgress) {
    if (newProgress < 0 || newProgress > requiredAmount) throw new IllegalArgumentException("invalid progress");
    if (status.terminal()) return this;
    return copyWith(newProgress, status == CommissionStatus.AVAILABLE ? CommissionStatus.ACTIVE : status);
  }

  public CommissionInstance addProgress(int delta) {
    if (delta < 0) throw new IllegalArgumentException("delta must be non-negative");
    long updated = (long) progress + delta;
    if (updated > requiredAmount) updated = requiredAmount;
    return withProgress((int) updated);
  }

  public CommissionInstance complete() {
    if (status == CommissionStatus.COMPLETED) return this;
    if (!completeable()) throw new IllegalStateException("commission is not completeable");
    return new CommissionInstance(
        commissionId,
        ownerPlayerId,
        templateId,
        type,
        requesterId,
        requesterName,
        targetSnapshot,
        requiredAmount,
        requiredAmount,
        rewardSnapshot,
        generatedAt,
        expiresAt,
        CommissionStatus.COMPLETED,
        text);
  }

  public CommissionInstance expireIfDue(long nowMillis) {
    if (nowMillis < expiresAt || status.terminal()) return this;
    return copyWith(progress, CommissionStatus.EXPIRED);
  }

  public CommissionInstance disable() {
    if (status == CommissionStatus.DISABLED) return this;
    if (status == CommissionStatus.COMPLETED || status == CommissionStatus.EXPIRED) return this;
    return copyWith(progress, CommissionStatus.DISABLED);
  }

  private CommissionInstance copyWith(int updatedProgress, CommissionStatus updatedStatus) {
    return new CommissionInstance(
        commissionId,
        ownerPlayerId,
        templateId,
        type,
        requesterId,
        requesterName,
        targetSnapshot,
        requiredAmount,
        updatedProgress,
        rewardSnapshot,
        generatedAt,
        expiresAt,
        updatedStatus,
        text);
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
    if (normalized.isEmpty() || normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }
}
