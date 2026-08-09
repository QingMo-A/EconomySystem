package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DeliveryState(
    List<DeliveryRow> rows,
    int page,
    int pageSize,
    String filter,
    ScreenState screenState,
    String errorKey,
    long requestId,
    Set<DeliveryAction> actions) {
  public DeliveryState {
    rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    if (page < 0 || pageSize < 1 || requestId < -1) throw new IllegalArgumentException("invalid delivery state");
    filter = Objects.requireNonNullElse(filter, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public List<DeliveryRow> filteredRows() {
    String needle = filter.trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) return rows;
    return rows.stream().filter(row -> {
      var entry = row.entry();
      return entry.entryId().toString().contains(needle)
          || entry.source().toLowerCase(Locale.ROOT).contains(needle)
          || entry.item().itemId().toLowerCase(Locale.ROOT).contains(needle);
    }).toList();
  }

  public int totalPages() {
    return Math.max(1, (filteredRows().size() + pageSize - 1) / pageSize);
  }

  public List<DeliveryRow> visibleRows() {
    List<DeliveryRow> values = filteredRows();
    int start = Math.min(page * pageSize, values.size());
    return values.subList(start, Math.min(values.size(), start + pageSize));
  }

  public boolean can(DeliveryAction action) {
    return actions.contains(action);
  }

  public DeliveryRow find(UUID entryId) {
    if (entryId == null) return null;
    return rows.stream().filter(row -> row.entryId().equals(entryId)).findFirst().orElse(null);
  }
}
