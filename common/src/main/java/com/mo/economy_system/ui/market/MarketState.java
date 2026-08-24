package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
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
    MarketOrderSort sort,
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
    sort = Objects.requireNonNull(sort, "sort");
    query = Objects.requireNonNullElse(query, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  /** Compatibility constructor for existing callers that still rely on DEFAULT sort. */
  public MarketState(
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
    this(rows, page, pageSize, totalMatched, totalSales, totalDemand, filter,
        MarketOrderSort.DEFAULT, query, screenState, errorKey, requestId, revision, actions);
  }

  public int totalPages() {
    return Math.max(1, (totalMatched + pageSize - 1) / pageSize);
  }

  public boolean can(MarketAction action) { return actions.contains(action); }

  public MarketRow find(UUID tradeId) {
    if (tradeId == null) return null;
    return rows.stream().filter(row -> row.order().tradeId().equals(tradeId)).findFirst().orElse(null);
  }

  /**
   * Applies the legacy client-side search predicate to already materialized rows. The wire
   * snapshot remains unchanged: a target may resolve a loader-native display name into
   * {@link MarketRow#displayName()} before the common controller receives the page.
   */
  public List<MarketRow> filteredRows() {
    String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
    if (needle.isEmpty()) return rows;
    return rows.stream().filter(row -> row.order().item().itemId().toLowerCase(java.util.Locale.ROOT).contains(needle)
        || row.order().ownerName().toLowerCase(java.util.Locale.ROOT).contains(needle)
        || row.order().tradeId().toString().toLowerCase(java.util.Locale.ROOT).contains(needle)
        || row.displayName().toLowerCase(java.util.Locale.ROOT).contains(needle)).toList();
  }
}
