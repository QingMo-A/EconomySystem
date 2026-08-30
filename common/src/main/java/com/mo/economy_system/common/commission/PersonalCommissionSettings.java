package com.mo.economy_system.common.commission;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Validated server configuration for each player's independent refresh schedule. */
public record PersonalCommissionSettings(
    long refreshBaseIntervalMillis,
    long refreshJitterMillis,
    int minCommissionsPerRefresh,
    int maxCommissionsPerRefresh,
    int maxActivePersonalCommissions,
    long defaultExpirationMinMillis,
    long defaultExpirationMaxMillis,
    double rewardMultiplierMin,
    double rewardMultiplierMax,
    Map<String, Integer> categoryWeights) {

  public PersonalCommissionSettings {
    if (refreshBaseIntervalMillis <= 0) throw new IllegalArgumentException("refresh interval must be positive");
    if (refreshJitterMillis < 0 || refreshJitterMillis >= refreshBaseIntervalMillis) {
      throw new IllegalArgumentException("jitter must be non-negative and smaller than interval");
    }
    if (minCommissionsPerRefresh <= 0 || maxCommissionsPerRefresh < minCommissionsPerRefresh) {
      throw new IllegalArgumentException("invalid commissions-per-refresh range");
    }
    if (maxActivePersonalCommissions <= 0) {
      throw new IllegalArgumentException("maxActivePersonalCommissions must be positive");
    }
    if (defaultExpirationMinMillis <= 0 || defaultExpirationMaxMillis < defaultExpirationMinMillis) {
      throw new IllegalArgumentException("invalid default expiration range");
    }
    if (!Double.isFinite(rewardMultiplierMin) || rewardMultiplierMin <= 0.0D
        || !Double.isFinite(rewardMultiplierMax) || rewardMultiplierMax < rewardMultiplierMin) {
      throw new IllegalArgumentException("invalid default reward multiplier range");
    }
    Objects.requireNonNull(categoryWeights, "categoryWeights");
    Map<String, Integer> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : categoryWeights.entrySet()) {
      String category = Objects.requireNonNull(entry.getKey(), "category").trim();
      Integer weight = Objects.requireNonNull(entry.getValue(), "category weight");
      if (category.isEmpty() || category.length() > 32 || weight <= 0) {
        throw new IllegalArgumentException("invalid category weight");
      }
      if (normalized.putIfAbsent(category, weight) != null) {
        throw new IllegalArgumentException("duplicate category: " + category);
      }
    }
    categoryWeights = Map.copyOf(normalized);
  }

  public PersonalCommissionSettings(
      long refreshBaseIntervalMillis,
      long refreshJitterMillis,
      int minCommissionsPerRefresh,
      int maxCommissionsPerRefresh,
      int maxActivePersonalCommissions,
      long defaultExpirationMinMillis,
      long defaultExpirationMaxMillis) {
    this(
        refreshBaseIntervalMillis,
        refreshJitterMillis,
        minCommissionsPerRefresh,
        maxCommissionsPerRefresh,
        maxActivePersonalCommissions,
        defaultExpirationMinMillis,
        defaultExpirationMaxMillis,
        1.0D,
        1.0D,
        Map.of());
  }

  public static PersonalCommissionSettings defaults() {
    return new PersonalCommissionSettings(
        4L * 60L * 60L * 1000L,
        30L * 60L * 1000L,
        1,
        2,
        6,
        2L * 60L * 60L * 1000L,
        4L * 60L * 60L * 1000L,
        0.9D,
        1.3D,
        Map.of("material", 60, "combat", 30, "rare", 10));
  }
}
