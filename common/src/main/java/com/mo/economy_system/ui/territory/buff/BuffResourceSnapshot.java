package com.mo.economy_system.ui.territory.buff;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Read-only client-side hint about resources; the server remains authoritative. */
public record BuffResourceSnapshot(Map<String, Integer> itemCounts, int experienceLevel,
                                   boolean known) {
  public BuffResourceSnapshot {
    Objects.requireNonNull(itemCounts, "itemCounts");
    if (experienceLevel < 0) throw new IllegalArgumentException("experienceLevel");
    LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isBlank()
          || entry.getValue() == null || entry.getValue() < 0) {
        throw new IllegalArgumentException("invalid item count");
      }
      copy.put(entry.getKey(), entry.getValue());
    }
    itemCounts = Collections.unmodifiableMap(copy);
  }

  public static BuffResourceSnapshot unknown() {
    return new BuffResourceSnapshot(Map.of(), 0, false);
  }

  public int itemCount(String itemId) {
    return itemCounts.getOrDefault(itemId, 0);
  }
}
