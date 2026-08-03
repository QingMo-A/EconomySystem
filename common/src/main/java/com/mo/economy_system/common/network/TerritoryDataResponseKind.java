package com.mo.economy_system.common.network;

import java.util.Arrays;

/** Stable protocol-18 result kind. Wire IDs are explicit and never enum ordinals. */
public enum TerritoryDataResponseKind {
  DATA("data"),
  ERROR("error");

  private final String id;

  TerritoryDataResponseKind(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static TerritoryDataResponseKind fromId(String id) {
    return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown territory response kind: " + id));
  }
}
