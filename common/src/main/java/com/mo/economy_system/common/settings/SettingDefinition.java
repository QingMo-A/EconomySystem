package com.mo.economy_system.common.settings;

import java.util.Objects;
import java.util.Set;

/** Loader-neutral typed setting definition with strict parsing. */
public record SettingDefinition(String key, String description, String defaultValue, Set<String> allowedValues) {
  public SettingDefinition {
    key = requireText(key, "key");
    description = Objects.requireNonNull(description, "description");
    defaultValue = normalize(defaultValue);
    allowedValues = Set.copyOf(Objects.requireNonNull(allowedValues, "allowedValues"));
    if (!allowedValues.isEmpty() && !allowedValues.contains(defaultValue)) {
      throw new IllegalArgumentException("default value is not allowed: " + defaultValue);
    }
  }

  public String parse(String raw) {
    String value = normalize(raw);
    if (!allowedValues.isEmpty() && !allowedValues.contains(value)) {
      throw new IllegalArgumentException("allowed values: " + String.join(", ", allowedValues));
    }
    return value;
  }

  private static String normalize(String value) {
    return Objects.requireNonNull(value, "value").trim().toLowerCase(java.util.Locale.ROOT);
  }

  private static String requireText(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException(name + " is blank");
    return normalized;
  }
}
