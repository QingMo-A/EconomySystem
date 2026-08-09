package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable state for the shared market list controller. */
public record MarketState(
    List<MarketRow> rows,
    int page,
    int pageSize,
    int totalMatched,
    int totalSales,
    int totalDemand,
    MarketOrderFilter filter,
    String query,
    ScreenState screenState,
    String errorKey,
    long requestId,
    long revision,
    Set<MarketAction> actions) {
  public MarketState {
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    if (page < 0 || pageSize < 1 || totalMatched < 0 || totalSales < 0 || totalDemand < 0
        || requestId < -1 || revision < -1) throw new IllegalArgumentException("invalid market state");
    filter = Objects.requireNonNull(filter, "filter");
    query = Objects.requireNonNullElse(query, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public int totalPages() {
    return Math.max(1, (totalMatched + pageSize - 1) / pageSize);
  }
  public boolean can(MarketAction action) { return actions.contains(action); }
  public MarketRow find(UUID tradeId) {
    if (tradeId == null) return null;
    return rows.stream().filter(row -> row.order().tradeId().equals(tradeId)).findFirst().orElse(null);
  }
}
