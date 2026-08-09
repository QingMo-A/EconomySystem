package com.mo.economy_system.common.reward;

import java.util.Objects;
import java.util.regex.Pattern;

/** One immutable, loader-neutral mob reward rule. */
public record RewardEntry(String type, double dropChance, int dropMin, int dropMax) {
  private static final int MAX_TYPE_LENGTH = 256;
  private static final Pattern TYPE_PATTERN =
      Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

  public RewardEntry {
    type = normalizeType(type);
    if (!Double.isFinite(dropChance) || dropChance < 0.0D || dropChance > 1.0D) {
      throw new IllegalArgumentException("dropChance must be finite and between 0 and 1");
    }
    if (dropMin < 0) throw new IllegalArgumentException("dropMin must be non-negative");
    if (dropMax < dropMin) throw new IllegalArgumentException("dropMax must be >= dropMin");
    if ((long) dropMax - dropMin + 1L > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("reward range is too wide");
    }
  }

  private static String normalizeType(String value) {
    String normalized = Objects.requireNonNull(value, "type").trim();
    if (normalized.isEmpty()
        || normalized.length() > MAX_TYPE_LENGTH
        || !TYPE_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException("invalid namespaced entity type: " + normalized);
    }
    return normalized;
  }
}
