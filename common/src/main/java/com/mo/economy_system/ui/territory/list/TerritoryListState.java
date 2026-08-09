package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable state rendered by both territory-list target shells. */
public record TerritoryListState(
    List<TerritoryListRow> rows,
    int page,
    int pageSize,
    String filter,
    ScreenState screenState,
    String errorKey,
    long requestId,
    Set<TerritoryListAction> actions) {
  public TerritoryListState {
    rows = List.copyOf(new ArrayList<>(Objects.requireNonNull(rows, "rows")));
    if (page < 0 || pageSize < 1 || requestId < -1) {
      throw new IllegalArgumentException("invalid territory list state");
    }
    filter = Objects.requireNonNullElse(filter, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public List<TerritoryListRow> filteredRows() {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) return rows;
    return rows.stream().filter(row -> row.summary().name().toLowerCase(Locale.ROOT).contains(needle)
        || row.summary().ownerName().toLowerCase(Locale.ROOT).contains(needle)
        || row.summary().territoryId().toString().contains(needle)
        || row.summary().dimensionId().toLowerCase(Locale.ROOT).contains(needle)).toList();
  }

  public int totalPages() {
    return Math.max(1, (filteredRows().size() + pageSize - 1) / pageSize);
  }

  public List<TerritoryListRow> visibleRows() {
    List<TerritoryListRow> filtered = filteredRows();
    int start = Math.min(page * pageSize, filtered.size());
    return filtered.subList(start, Math.min(filtered.size(), start + pageSize));
  }

  public boolean can(TerritoryListAction action) {
    return actions.contains(action);
  }

  public TerritoryListRow find(UUID territoryId) {
    if (territoryId == null) return null;
    return rows.stream().filter(row -> row.summary().territoryId().equals(territoryId)).findFirst().orElse(null);
  }
}
