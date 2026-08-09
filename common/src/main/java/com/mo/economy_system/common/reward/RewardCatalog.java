package com.mo.economy_system.common.reward;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Atomically replaceable reward lookup used by the config watcher and server services. */
public final class RewardCatalog {
  private volatile Map<String, RewardEntry> entries;

  public RewardCatalog(List<RewardEntry> entries) {
    this.entries = index(entries);
  }

  public Optional<RewardEntry> find(String entityType) {
    if (entityType == null) return Optional.empty();
    return Optional.ofNullable(entries.get(entityType));
  }

  public List<RewardEntry> snapshot() {
    return List.copyOf(entries.values());
  }

  public synchronized void replace(List<RewardEntry> replacement) {
    entries = index(replacement);
  }

  private static Map<String, RewardEntry> index(List<RewardEntry> source) {
    Objects.requireNonNull(source, "source");
    Map<String, RewardEntry> indexed = new LinkedHashMap<>();
    for (RewardEntry entry : source) {
      Objects.requireNonNull(entry, "entry");
      if (indexed.putIfAbsent(entry.type(), entry) != null) {
        throw new IllegalArgumentException("duplicate reward entity type: " + entry.type());
      }
    }
    return Map.copyOf(indexed);
  }
}
