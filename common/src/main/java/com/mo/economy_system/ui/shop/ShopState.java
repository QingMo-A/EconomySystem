package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable state for the common shop controller. */
public record ShopState(
    List<ShopRow> rows,
    int page,
    int pageSize,
    String filter,
    ScreenState screenState,
    String errorKey,
    long requestId,
    Set<ShopAction> actions) {
  public ShopState {
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    if (page < 0 || pageSize < 1 || requestId < -1) throw new IllegalArgumentException("invalid shop state");
    filter = Objects.requireNonNullElse(filter, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public List<ShopRow> filteredRows() {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) return rows;
    return rows.stream().filter(row -> {
      var item = row.item();
      return item.shopItemId().toLowerCase(Locale.ROOT).contains(needle)
          || item.itemId().toLowerCase(Locale.ROOT).contains(needle)
          || item.description().toLowerCase(Locale.ROOT).contains(needle)
          || row.displayName().toLowerCase(Locale.ROOT).contains(needle);
    }).toList();
  }

  public int totalPages() {
    return Math.max(1, (filteredRows().size() + pageSize - 1) / pageSize);
  }

  public List<ShopRow> visibleRows() {
    List<ShopRow> filtered = filteredRows();
    int start = Math.min(page * pageSize, filtered.size());
    return filtered.subList(start, Math.min(filtered.size(), start + pageSize));
  }

  public boolean can(ShopAction action) {
    return actions.contains(action);
  }

  public ShopRow find(String shopItemId) {
    if (shopItemId == null) return null;
    return rows.stream().filter(row -> row.item().shopItemId().equals(shopItemId)).findFirst().orElse(null);
  }
}
