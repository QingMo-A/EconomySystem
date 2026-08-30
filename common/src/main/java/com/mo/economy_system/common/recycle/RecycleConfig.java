package com.mo.economy_system.common.recycle;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loader-neutral, validated configuration for the server recycling station. */
public record RecycleConfig(Duration cycle, List<RecycleOffer> offers) {
  public RecycleConfig {
    cycle = Objects.requireNonNull(cycle, "cycle");
    offers = List.copyOf(Objects.requireNonNull(offers, "offers"));
    if (cycle.isZero() || cycle.isNegative()) throw new IllegalArgumentException("cycle must be positive");
    Map<String, Boolean> ids = new LinkedHashMap<>();
    for (RecycleOffer offer : offers) {
      Objects.requireNonNull(offer, "offers contains null");
      if (ids.put(offer.itemId(), Boolean.TRUE) != null) {
        throw new IllegalArgumentException("duplicate recycle item: " + offer.itemId());
      }
    }
  }

  public RecycleConfig(Duration cycle, RecycleOffer... offers) {
    this(cycle, List.of(offers));
  }

  public Map<String, RecycleOffer> byItemId() {
    Map<String, RecycleOffer> values = new LinkedHashMap<>();
    offers.forEach(offer -> values.put(offer.itemId(), offer));
    return Map.copyOf(values);
  }

  public static RecycleConfig defaults() {
    return new RecycleConfig(Duration.ofHours(1), List.of(
        new RecycleOffer("minecraft:rotten_flesh", 1),
        new RecycleOffer("minecraft:cobblestone", 1),
        new RecycleOffer("minecraft:spider_eye", 1),
        new RecycleOffer("minecraft:kelp", 1, 2, 1536, true)));
  }
}
