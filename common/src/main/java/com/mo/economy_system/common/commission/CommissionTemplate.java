package com.mo.economy_system.common.commission;

import java.util.Objects;

/**
 * An administrator-authored question in the personal commission pool.
 *
 * <p>Templates are never exposed as player-owned state. {@link CommissionGenerator} resolves all
 * references and freezes the resulting values in a {@link CommissionInstance}.
 */
public record CommissionTemplate(
    String id,
    CommissionType type,
    String requesterPool,
    String targetPool,
    int quantityMin,
    int quantityMax,
    int quantityStep,
    CommissionRewardMode rewardMode,
    int rewardPerUnit,
    double rewardMultiplierMin,
    double rewardMultiplierMax,
    int weight,
    String category,
    String rarity,
    long expirationMinMillis,
    long expirationMaxMillis,
    int playerLimit,
    String textTemplate,
    String requiredProfession,
    int requiredProfessionLevel,
    int professionExperienceReward) {

  public CommissionTemplate {
    id = identifier(id, "id");
    Objects.requireNonNull(type, "type");
    requesterPool = identifier(requesterPool, "requesterPool");
    targetPool = identifier(targetPool, "targetPool");
    if (quantityMin <= 0 || quantityMax < quantityMin) {
      throw new IllegalArgumentException("invalid quantity range");
    }
    if (quantityStep <= 0) throw new IllegalArgumentException("quantityStep must be positive");
    if (rewardMode == null) throw new NullPointerException("rewardMode");
    if (rewardPerUnit < 0) throw new IllegalArgumentException("rewardPerUnit must be non-negative");
    if (!Double.isFinite(rewardMultiplierMin) || rewardMultiplierMin <= 0.0D
        || !Double.isFinite(rewardMultiplierMax)
        || rewardMultiplierMax < rewardMultiplierMin) {
      throw new IllegalArgumentException("invalid reward multiplier range");
    }
    if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
    category = text(category, "category", 32);
    rarity = text(rarity, "rarity", 32);
    if (expirationMinMillis <= 0 || expirationMaxMillis < expirationMinMillis) {
      throw new IllegalArgumentException("invalid expiration range");
    }
    if (playerLimit == 0 || playerLimit < -1) {
      throw new IllegalArgumentException("playerLimit must be -1 or positive");
    }
    textTemplate = Objects.requireNonNullElse(textTemplate, "");
    if (textTemplate.length() > 1024) throw new IllegalArgumentException("textTemplate exceeds limit");
    requiredProfession = optionalText(requiredProfession, 64);
    if (requiredProfessionLevel < 0) {
      throw new IllegalArgumentException("requiredProfessionLevel must be non-negative");
    }
    if (professionExperienceReward < 0) {
      throw new IllegalArgumentException("professionExperienceReward must be non-negative");
    }
  }

  /** Minimal constructor for a fixed one-currency reward template. */
  public CommissionTemplate(
      String id,
      CommissionType type,
      String requesterPool,
      String targetPool,
      int quantityMin,
      int quantityMax,
      int quantityStep,
      CommissionRewardMode rewardMode,
      int rewardPerUnit,
      int weight,
      String category,
      String rarity,
      long expirationMinMillis,
      long expirationMaxMillis) {
    this(
        id,
        type,
        requesterPool,
        targetPool,
        quantityMin,
        quantityMax,
        quantityStep,
        rewardMode,
        rewardPerUnit,
        1.0D,
        1.0D,
        weight,
        category,
        rarity,
        expirationMinMillis,
        expirationMaxMillis,
        -1,
        "",
        "",
        0,
        0);
  }

  /** Compact constructor useful in tests and small programmatic catalogs. */
  public CommissionTemplate(
      String id,
      CommissionType type,
      String requesterPool,
      String targetPool,
      int quantityMin,
      int quantityMax,
      int quantityStep,
      int rewardPerUnit,
      int weight,
      String category,
      long expirationMinMillis,
      long expirationMaxMillis) {
    this(
        id,
        type,
        requesterPool,
        targetPool,
        quantityMin,
        quantityMax,
        quantityStep,
        CommissionRewardMode.PER_UNIT,
        rewardPerUnit,
        1.0D,
        1.0D,
        weight,
        category,
        "common",
        expirationMinMillis,
        expirationMaxMillis,
        -1,
        "",
        "",
        0,
        0);
  }

  public boolean allowsPlayerCount(int generatedForPlayerCount) {
    return playerLimit < 0 || generatedForPlayerCount < playerLimit;
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

  private static String optionalText(String value, int maxLength) {
    String normalized = Objects.requireNonNullElse(value, "").trim();
    if (normalized.length() > maxLength) throw new IllegalArgumentException("text exceeds limit");
    return normalized;
  }
}
