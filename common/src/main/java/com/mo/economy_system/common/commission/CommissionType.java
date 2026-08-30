package com.mo.economy_system.common.commission;

import java.util.Locale;

/** Loader-neutral kind of a commission objective. */
public enum CommissionType {
  ITEM_DELIVERY,
  ENTITY_KILL,
  EXPLORATION,
  CUSTOM;

  public static CommissionType fromId(String raw) {
    if (raw == null) throw new IllegalArgumentException("commission type is required");
    String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("unknown commission type: " + raw, error);
    }
  }

  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }
}
