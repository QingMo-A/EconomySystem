package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderSnapshot;
import java.util.Objects;

/** Immutable market card row. */
public record MarketRow(MarketOrderSnapshot order, String displayName) {
  /** Compatibility constructor for tests and non-client callers without a loader. */
  public MarketRow(MarketOrderSnapshot order) { this(order, ""); }

  public MarketRow {
    Objects.requireNonNull(order, "order");
    displayName = Objects.requireNonNullElse(displayName, "").trim();
  }
}
