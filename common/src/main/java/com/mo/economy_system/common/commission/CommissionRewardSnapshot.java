package com.mo.economy_system.common.commission;

import java.util.Objects;

/**
 * Immutable reward quote frozen when a commission is generated.
 *
 * <p>The snapshot is only a domain value. It never mutates an account and does not model a
 * mailbox attachment; a target adapter turns a pending reward record into its own claimable
 * currency attachment later.
 */
public record CommissionRewardSnapshot(String currencyId, int amount, String description) {
  public static final String DEFAULT_CURRENCY_ID = "economysystem:money";

  public CommissionRewardSnapshot {
    currencyId = normalize(currencyId, "currencyId", 64);
    if (amount < 0) throw new IllegalArgumentException("reward amount must be non-negative");
    description = Objects.requireNonNullElse(description, "");
    if (description.length() > 256) throw new IllegalArgumentException("description exceeds limit");
  }

  public CommissionRewardSnapshot(int amount) {
    this(DEFAULT_CURRENCY_ID, amount, "");
  }

  public static CommissionRewardSnapshot coins(int amount) {
    return new CommissionRewardSnapshot(amount);
  }

  private static String normalize(String value, String field, int maxLength) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }
}
