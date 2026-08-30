package com.mo.economy_system.common.commission;

import java.util.Locale;

/** How a template's base reward is applied to its generated quantity. */
public enum CommissionRewardMode {
  FIXED,
  PER_UNIT;

  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static CommissionRewardMode fromId(String raw) {
    if (raw == null) throw new IllegalArgumentException("reward mode is required");
    String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("unknown reward mode: " + raw, error);
    }
  }
}
