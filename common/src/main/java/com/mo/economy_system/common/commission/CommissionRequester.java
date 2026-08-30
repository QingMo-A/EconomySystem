package com.mo.economy_system.common.commission;

import java.util.Objects;

/** An immutable requester identity and its generation modifiers. */
public record CommissionRequester(
    String id,
    String displayName,
    double quantityMultiplier,
    double rewardMultiplier,
    int weight,
    String rarity,
    String mailSignature) {

  public CommissionRequester {
    id = identifier(id, "id");
    displayName = text(displayName, "displayName", 128);
    if (!Double.isFinite(quantityMultiplier) || quantityMultiplier <= 0.0D) {
      throw new IllegalArgumentException("quantityMultiplier must be finite and positive");
    }
    if (!Double.isFinite(rewardMultiplier) || rewardMultiplier <= 0.0D) {
      throw new IllegalArgumentException("rewardMultiplier must be finite and positive");
    }
    if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
    rarity = text(rarity, "rarity", 32);
    mailSignature = Objects.requireNonNullElse(mailSignature, "");
    if (mailSignature.length() > 256) throw new IllegalArgumentException("mailSignature exceeds limit");
  }

  public CommissionRequester(String id, String displayName) {
    this(id, displayName, 1.0D, 1.0D, 1, "common", "");
  }

  public CommissionRequester(
      String id,
      String displayName,
      double quantityMultiplier,
      double rewardMultiplier,
      int weight,
      String rarity) {
    this(id, displayName, quantityMultiplier, rewardMultiplier, weight, rarity, "");
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
