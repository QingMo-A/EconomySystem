package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderSnapshot;
import java.util.Objects;

/** Immutable market card row. */
public record MarketRow(MarketOrderSnapshot order) {
  public MarketRow {
    Objects.requireNonNull(order, "order");
  }
}
